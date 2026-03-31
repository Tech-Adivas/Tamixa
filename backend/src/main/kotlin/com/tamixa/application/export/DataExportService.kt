package com.tamixa.application.export

import com.tamixa.api.config.RequestTracingFilter
import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.DataExportStoragePort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import com.tamixa.infrastructure.persistence.DataExportJobEntity
import com.tamixa.infrastructure.persistence.DataExportJobJpaRepository
import com.tamixa.infrastructure.persistence.FavoriteStoryJpaRepository
import com.tamixa.infrastructure.persistence.ParentConsentJpaRepository
import com.tamixa.infrastructure.persistence.ParentEntity
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.StoryEntity
import com.tamixa.infrastructure.persistence.StoryJpaRepository
import com.fasterxml.jackson.core.JsonEncoding
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class ExportJobStatus(val id: Long, val status: String, val requestedAt: Instant, val downloadUrl: String?)

@Service
class DataExportService(
    private val parentJpaRepository: ParentJpaRepository,
    private val jobJpaRepository: DataExportJobJpaRepository,
    private val storyJpaRepository: StoryJpaRepository,
    private val favoriteStoryJpaRepository: FavoriteStoryJpaRepository,
    private val consentJpaRepository: ParentConsentJpaRepository,
    private val auditLog: AuditLogPort,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
    private val appProperties: AppProperties,
    storageProvider: ObjectProvider<DataExportStoragePort>,
    signedUrlGeneratorProvider: ObjectProvider<S3SignedUrlGenerator>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val storage: DataExportStoragePort? = storageProvider.getIfAvailable()
    private val signedUrlGenerator: S3SignedUrlGenerator? = signedUrlGeneratorProvider.getIfAvailable()

    fun requestExport(parentEmail: String): ExportJobStatus {
        val parent = parentJpaRepository.findByEmail(parentEmail)
            ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        val entity = DataExportJobEntity(
            parent = parent,
            status = "pending",
            requestedAt = Instant.now()
        )
        val saved = jobJpaRepository.save(entity)
        log.info("Data export requested: parentId={} jobId={}", parent.id, saved.id)
        auditLog.logDataExportRequest(parent.id, saved.id, MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY))
        return toStatus(saved)
    }

    fun listJobs(parentEmail: String, limit: Int = 10): List<ExportJobStatus> {
        val parent = parentJpaRepository.findByEmail(parentEmail)
            ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        val jobs = jobJpaRepository.findByParent_IdOrderByRequestedAtDesc(parent.id, PageRequest.of(0, limit))
            .content
        return jobs.map { toStatus(it) }
    }

    private fun toStatus(entity: DataExportJobEntity): ExportJobStatus {
        val downloadUrl = when {
            entity.status == "completed" && entity.storageKey != null && signedUrlGenerator != null ->
                signedUrlGenerator.signUrl(entity.storageKey!!, appProperties.export.signedUrlExpiryMinutes)?.toString()
            else -> null
        }
        return ExportJobStatus(entity.id, entity.status, entity.requestedAt, downloadUrl)
    }

    /**
     * Process one pending export job. Called by scheduled job or on-demand.
     * Requires DataExportStoragePort (S3) to be configured.
     */
    @Transactional
    fun processNextPendingJob(): Boolean {
        if (storage == null) {
            log.debug("Data export skipped: storage not configured (app.storage.type=s3 required)")
            return false
        }
        val page = jobJpaRepository.findByStatus("pending", PageRequest.of(0, 1))
        val job = page.content.firstOrNull() ?: return false
        return try {
            processJob(job)
            true
        } catch (e: Exception) {
            log.error("Data export failed jobId={}: {}", job.id, e.message, e)
            job.status = "failed"
            job.completedAt = Instant.now()
            jobJpaRepository.save(job)
            false
        }
    }

    private fun processJob(job: DataExportJobEntity) {
        val parent = job.parent
        val exportCfg = appProperties.export
        val storyPageSize = exportCfg.storyPageSize.coerceIn(50, 500)
        val consentPageSize = exportCfg.consentPageSize.coerceIn(20, 500)
        val path = Files.createTempFile("tamixa-export-${job.id}-", ".json")
        try {
            Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING).use { out ->
                val gen = objectMapper.factory.createGenerator(out, JsonEncoding.UTF8)
                gen.use { g ->
                    g.writeStartObject()
                    g.writeStringField(
                        "exportedAt",
                        Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
                    )
                    g.writeFieldName("parent")
                    objectMapper.writeValue(g, toParentExport(parent))
                    g.writeArrayFieldStart("children")
                    g.writeEndArray()
                    g.writeArrayFieldStart("stories")
                    var sp = 0
                    while (true) {
                        val page = storyJpaRepository.findByParent_Id(parent.id, PageRequest.of(sp, storyPageSize))
                        for (s in page.content) {
                            objectMapper.writeValue(g, toStoryExport(s))
                        }
                        if (!page.hasNext()) break
                        sp++
                    }
                    g.writeEndArray()
                    g.writeArrayFieldStart("favorites")
                    for (f in favoriteStoryJpaRepository.findByParent_Id(parent.id)) {
                        objectMapper.writeValue(g, FavoriteExport(f.storyId, f.storySource))
                    }
                    g.writeEndArray()
                    g.writeArrayFieldStart("consents")
                    var cp = 0
                    while (true) {
                        val cpage = consentJpaRepository.findByParent_IdOrderByGrantedAtDesc(
                            parent.id,
                            PageRequest.of(cp, consentPageSize)
                        )
                        for (c in cpage.content) {
                            objectMapper.writeValue(
                                g,
                                ConsentExport(
                                    c.consentType,
                                    c.version,
                                    c.grantedAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
                                )
                            )
                        }
                        if (!cpage.hasNext()) break
                        cp++
                    }
                    g.writeEndArray()
                    g.writeEndObject()
                }
            }
            val len = Files.size(path)
            val storageKey = Files.newInputStream(path).use { stream ->
                storage!!.uploadExportStream(parent.id, job.id, stream, len)
            }
            val expiresAt = Instant.now().plusSeconds(24L * 60 * 60 * appProperties.export.expiryDays)
            job.status = "completed"
            job.completedAt = Instant.now()
            job.storageKey = storageKey
            job.expiresAt = expiresAt
            jobJpaRepository.save(job)
            log.info("Data export completed: parentId={} jobId={} bytes={}", parent.id, job.id, len)
        } finally {
            try {
                Files.deleteIfExists(path)
            } catch (e: Exception) {
                log.warn("Failed to delete temp export file jobId={}: {}", job.id, e.message)
            }
        }
    }

    private fun toParentExport(p: ParentEntity) = ParentExport(
        id = p.id,
        email = p.email,
        phone = p.phone,
        createdAt = p.createdAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
        role = p.role.name
    )

    private fun toStoryExport(s: StoryEntity) = StoryExport(
        id = s.id,
        theme = s.theme,
        language = s.language,
        age = s.age,
        childName = s.childName,
        title = s.title,
        moral = s.moral,
        wordCount = s.wordCount,
        readingTimeMinutes = s.readingTimeMinutes,
        status = s.status.name,
        createdAt = s.createdAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
    )
}
