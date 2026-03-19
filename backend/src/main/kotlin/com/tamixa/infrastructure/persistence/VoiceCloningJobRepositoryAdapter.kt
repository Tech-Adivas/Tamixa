package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.VoiceCloningJobRepositoryPort
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus
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
            consentAudioStoragePath = job.consentAudioStoragePath,
            audioFileSizeBytes = job.audioFileSizeBytes,
            voiceName = job.voiceName,
            elevenLabsVoiceId = job.elevenLabsVoiceId,
            googleVoiceCloningKey = job.googleVoiceCloningKey,
            status = job.status,
            errorMessage = job.errorMessage,
            createdAt = job.createdAt,
            completedAt = job.completedAt
        )
        val saved = voiceCloningJobJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): VoiceCloningJob? {
        return voiceCloningJobJpaRepository.findById(id).map { toDomain(it) }.orElse(null)
    }

    override fun findByParentId(parentId: Long): List<VoiceCloningJob> {
        return voiceCloningJobJpaRepository.findByParentId(parentId).map { toDomain(it) }
    }

    override fun findByStatus(status: VoiceCloningStatus): List<VoiceCloningJob> {
        return voiceCloningJobJpaRepository.findByStatus(status).map { toDomain(it) }
    }

    private fun toDomain(it: VoiceCloningJobEntity) = VoiceCloningJob(
        id = it.id,
        parentId = it.parentId,
        audioStoragePath = it.audioStoragePath,
        consentAudioStoragePath = it.consentAudioStoragePath,
        audioFileSizeBytes = it.audioFileSizeBytes,
        voiceName = it.voiceName,
        elevenLabsVoiceId = it.elevenLabsVoiceId,
        googleVoiceCloningKey = it.googleVoiceCloningKey,
        status = it.status,
        errorMessage = it.errorMessage,
        createdAt = it.createdAt,
        completedAt = it.completedAt
    )

    @Transactional
    override fun updateStatus(
        id: Long,
        status: VoiceCloningStatus,
        errorMessage: String?,
        elevenLabsVoiceId: String?,
        googleVoiceCloningKey: String?
    ) {
        val now = Instant.now()
        voiceCloningJobJpaRepository.updateStatus(id, status, elevenLabsVoiceId, googleVoiceCloningKey, errorMessage, now)
    }
}
