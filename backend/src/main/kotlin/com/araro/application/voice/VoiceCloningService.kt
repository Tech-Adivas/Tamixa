package com.araro.application.voice

import com.araro.application.port.VoiceCloningJobRepositoryPort
import com.araro.application.port.ParentRepositoryPort
import com.araro.application.port.voice.ElevenLabsVoiceCloningPort
import com.araro.domain.VoiceCloningJob
import com.araro.domain.VoiceCloningStatus
import com.araro.infrastructure.config.AppProperties
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class VoiceCloningService(
    private val voiceCloningJobRepository: VoiceCloningJobRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val elevenLabsAdapter: ElevenLabsVoiceCloningPort,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val voiceCloningConfig get() = appProperties.voiceCloning

    @Transactional
    fun createVoiceCloningJob(
        parentId: Long,
        audioStoragePath: String,
        audioFileSizeBytes: Long,
        voiceName: String
    ): VoiceCloningJob {
        val job = VoiceCloningJob(
            id = 0,
            parentId = parentId,
            audioStoragePath = audioStoragePath,
            audioFileSizeBytes = audioFileSizeBytes,
            voiceName = voiceName,
            elevenLabsVoiceId = null,
            status = VoiceCloningStatus.PENDING,
            errorMessage = null,
            createdAt = Instant.now(),
            completedAt = null
        )
        return voiceCloningJobRepository.save(job)
    }

    @Async
    @Transactional
    fun processVoiceCloningJob(jobId: Long) {
        val job = voiceCloningJobRepository.findById(jobId)
            ?: throw VoiceCloningJobNotFoundException(jobId)

        if (job.status != VoiceCloningStatus.PENDING) {
            log.warn("Job already processed: jobId={}", jobId)
            return
        }

        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.PROCESSING, null, null)

        try {
            // Read audio from storage (placeholder - would need storage adapter)
            val audioBytes = byteArrayOf() // In production, fetch from storage
            if (audioBytes.isEmpty()) {
                throw IllegalStateException("Audio file not found: ${job.audioStoragePath}")
            }

            // Call ElevenLabs to create voice
            val voiceId = elevenLabsAdapter.addVoice(
                audioBytes = audioBytes,
                fileName = "voice_${job.voiceName}.mp3",
                name = job.voiceName
            )

            if (voiceId == null) {
                throw IllegalStateException("ElevenLabs voice creation failed")
            }

            voiceCloningJobRepository.updateStatus(
                jobId,
                VoiceCloningStatus.READY,
                null,
                voiceId
            )

            log.info(
                "Voice cloning completed: jobId={} voiceId={}",
                jobId,
                voiceId.take(8),
                StructuredArguments.kv("parentId", job.parentId)
            )
        } catch (e: Exception) {
            log.error("Voice cloning failed: jobId={}", jobId, e)
            voiceCloningJobRepository.updateStatus(
                jobId,
                VoiceCloningStatus.FAILED,
                e.message ?: "Unknown error",
                null
            )
        }
    }

    fun getVoiceCloningJobs(parentId: Long): List<VoiceCloningJob> {
        return voiceCloningJobRepository.findByParentId(parentId)
    }

    fun getVoiceCloningJob(jobId: Long): VoiceCloningJob? {
        return voiceCloningJobRepository.findById(jobId)
    }

    fun getReadyVoices(parentId: Long): List<VoiceCloningJob> {
        return voiceCloningJobRepository.findByParentId(parentId)
            .filter { it.status == VoiceCloningStatus.READY }
    }

    @Transactional
    fun uploadVoice(parentEmail: String, audioBytes: ByteArray, fileName: String): VoiceCloningJob {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw com.araro.application.child.ParentNotFoundException(parentEmail)
        
        val job = VoiceCloningJob(
            id = 0,
            parentId = parent.id,
            audioStoragePath = "temp/${parent.id}/${fileName}",
            audioFileSizeBytes = audioBytes.size.toLong(),
            voiceName = fileName.removeSuffix(".${fileName.split(".").last()}"),
            elevenLabsVoiceId = null,
            status = VoiceCloningStatus.PENDING,
            errorMessage = null,
            createdAt = Instant.now(),
            completedAt = null
        )
        return voiceCloningJobRepository.save(job)
    }
}

class VoiceCloningJobNotFoundException(id: Long) : RuntimeException("Voice cloning job not found: $id")
