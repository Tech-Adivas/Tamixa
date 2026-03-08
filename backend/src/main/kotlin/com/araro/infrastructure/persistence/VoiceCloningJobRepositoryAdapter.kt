package com.araro.infrastructure.persistence

import com.araro.application.port.VoiceCloningJobRepositoryPort
import com.araro.domain.VoiceCloningJob
import com.araro.domain.VoiceCloningStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class VoiceCloningJobRepositoryAdapter(
    private val voiceCloningJobJpaRepository: VoiceCloningJobJpaRepository
) : VoiceCloningJobRepositoryPort {

    @Transactional
    override fun save(job: VoiceCloningJob): VoiceCloningJob {
        val entity = VoiceCloningJobEntity(
            id = job.id,
            parentId = job.parentId,
            audioStoragePath = job.audioStoragePath,
            audioFileSizeBytes = job.audioFileSizeBytes,
            voiceName = job.voiceName,
            elevenLabsVoiceId = job.elevenLabsVoiceId,
            status = job.status,
            errorMessage = job.errorMessage,
            createdAt = job.createdAt,
            completedAt = job.completedAt
        )
        val saved = voiceCloningJobJpaRepository.save(entity)
        return VoiceCloningJob(
            id = saved.id,
            parentId = saved.parentId,
            audioStoragePath = saved.audioStoragePath,
            audioFileSizeBytes = saved.audioFileSizeBytes,
            voiceName = saved.voiceName,
            elevenLabsVoiceId = saved.elevenLabsVoiceId,
            status = saved.status,
            errorMessage = saved.errorMessage,
            createdAt = saved.createdAt,
            completedAt = saved.completedAt
        )
    }

    override fun findById(id: Long): VoiceCloningJob? {
        return voiceCloningJobJpaRepository.findById(id).map {
            VoiceCloningJob(
                id = it.id,
                parentId = it.parentId,
                audioStoragePath = it.audioStoragePath,
                audioFileSizeBytes = it.audioFileSizeBytes,
                voiceName = it.voiceName,
                elevenLabsVoiceId = it.elevenLabsVoiceId,
                status = it.status,
                errorMessage = it.errorMessage,
                createdAt = it.createdAt,
                completedAt = it.completedAt
            )
        }.orElse(null)
    }

    override fun findByParentId(parentId: Long): List<VoiceCloningJob> {
        return voiceCloningJobJpaRepository.findByParentId(parentId).map {
            VoiceCloningJob(
                id = it.id,
                parentId = it.parentId,
                audioStoragePath = it.audioStoragePath,
                audioFileSizeBytes = it.audioFileSizeBytes,
                voiceName = it.voiceName,
                elevenLabsVoiceId = it.elevenLabsVoiceId,
                status = it.status,
                errorMessage = it.errorMessage,
                createdAt = it.createdAt,
                completedAt = it.completedAt
            )
        }
    }

    override fun findByStatus(status: VoiceCloningStatus): List<VoiceCloningJob> {
        return voiceCloningJobJpaRepository.findByStatus(status).map {
            VoiceCloningJob(
                id = it.id,
                parentId = it.parentId,
                audioStoragePath = it.audioStoragePath,
                audioFileSizeBytes = it.audioFileSizeBytes,
                voiceName = it.voiceName,
                elevenLabsVoiceId = it.elevenLabsVoiceId,
                status = it.status,
                errorMessage = it.errorMessage,
                createdAt = it.createdAt,
                completedAt = it.completedAt
            )
        }
    }

    @Transactional
    override fun updateStatus(
        id: Long,
        status: VoiceCloningStatus,
        errorMessage: String?,
        elevenLabsVoiceId: String?
    ) {
        val now = Instant.now()
        voiceCloningJobJpaRepository.updateStatus(id, status, elevenLabsVoiceId, errorMessage, now)
    }
}
