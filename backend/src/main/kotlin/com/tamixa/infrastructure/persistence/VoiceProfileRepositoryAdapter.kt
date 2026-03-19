package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.domain.VoiceProfile
import org.springframework.stereotype.Component

@Component
class VoiceProfileRepositoryAdapter(
    private val jpaRepository: VoiceProfileJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : VoiceRepositoryPort {

    override fun save(voiceProfile: VoiceProfile): VoiceProfile {
        val parentEntity = parentJpaRepository.findById(voiceProfile.parentId).orElseThrow {
            IllegalArgumentException("Parent not found: ${voiceProfile.parentId}")
        }
        val entity = VoiceProfileEntity(
            id = voiceProfile.id.takeIf { it > 0 } ?: 0,
            parent = parentEntity,
            encryptedEmbedding = voiceProfile.encryptedEmbedding,
            createdAt = voiceProfile.createdAt,
            elevenlabsVoiceId = voiceProfile.elevenlabsVoiceId,
            googleVoiceCloningKey = voiceProfile.googleVoiceCloningKey,
            referenceAudioPath = voiceProfile.referenceAudioPath,
            heygenVoiceId = voiceProfile.heygenVoiceId
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): VoiceProfile? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByParentId(parentId: Long): List<VoiceProfile> {
        return jpaRepository.findByParent_Id(parentId).map { it.toDomain() }
    }

    override fun findByIdAndParentId(id: Long, parentId: Long): VoiceProfile? {
        return jpaRepository.findByIdAndParent_Id(id, parentId)?.toDomain()
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}

private fun VoiceProfileEntity.toDomain(): VoiceProfile = VoiceProfile(
    id = id,
    parentId = parent.id,
    encryptedEmbedding = encryptedEmbedding,
    createdAt = createdAt,
    elevenlabsVoiceId = elevenlabsVoiceId,
    googleVoiceCloningKey = googleVoiceCloningKey,
    referenceAudioPath = referenceAudioPath,
    heygenVoiceId = heygenVoiceId
)
