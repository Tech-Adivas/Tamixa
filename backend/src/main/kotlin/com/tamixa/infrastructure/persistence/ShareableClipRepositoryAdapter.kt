package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ShareableClip
import com.tamixa.application.port.ShareableClipRepositoryPort
import org.springframework.stereotype.Component

@Component
class ShareableClipRepositoryAdapter(
    private val jpaRepository: ShareableClipJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : ShareableClipRepositoryPort {

    override fun save(clip: ShareableClip): ShareableClip {
        val parent = parentJpaRepository.findById(clip.parentId).orElseThrow { IllegalArgumentException("Parent not found: ${clip.parentId}") }
        val entity = ShareableClipEntity(
            id = if (clip.id == 0L) 0 else clip.id,
            parent = parent,
            storyId = clip.storyId,
            storySource = clip.storySource,
            language = clip.language,
            voiceProfile = clip.voiceProfile,
            startSeconds = clip.startSeconds,
            durationSeconds = clip.durationSeconds,
            format = clip.format,
            storagePath = clip.storagePath,
            status = clip.status,
            processingJobId = clip.processingJobId,
            errorMessage = clip.errorMessage,
            createdAt = clip.createdAt,
            updatedAt = clip.updatedAt,
            downloadAnalyticsEmittedAt = clip.downloadAnalyticsEmittedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): ShareableClip? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByIdAndParentId(id: Long, parentId: Long): ShareableClip? =
        jpaRepository.findByIdAndParent_Id(id, parentId)?.toDomain()

    override fun countByParentIdSince(parentId: Long, since: java.time.Instant): Int =
        jpaRepository.findByParent_IdAndCreatedAtAfter(parentId, since).size

    override fun updateStatusAndStorage(id: Long, status: String, storagePath: String?, errorMessage: String?) {
        jpaRepository.findById(id).ifPresent { entity ->
            entity.status = status
            entity.storagePath = storagePath
            entity.errorMessage = errorMessage
            entity.updatedAt = java.time.Instant.now()
            jpaRepository.save(entity)
        }
    }

    override fun markDownloadAnalyticsEmitted(id: Long): Boolean =
        jpaRepository.markDownloadAnalyticsEmittedIfUnset(id, java.time.Instant.now()) > 0
}

private fun ShareableClipEntity.toDomain() = ShareableClip(
    id = id,
    parentId = parent.id,
    storyId = storyId,
    storySource = storySource,
    language = language,
    voiceProfile = voiceProfile,
    startSeconds = startSeconds,
    durationSeconds = durationSeconds,
    format = format,
    storagePath = storagePath,
    status = status,
    processingJobId = processingJobId,
    errorMessage = errorMessage,
    createdAt = createdAt,
    updatedAt = updatedAt,
    downloadAnalyticsEmittedAt = downloadAnalyticsEmittedAt
)
