package com.araro.infrastructure.persistence

import com.araro.application.port.narration.StoryNarrationAudioRepositoryPort
import com.araro.domain.narration.NarrationAudioStatus
import com.araro.domain.narration.StoryNarrationAudio
import org.springframework.stereotype.Component

@Component
class StoryNarrationAudioRepositoryAdapter(
    private val jpaRepository: StoryNarrationAudioJpaRepository
) : StoryNarrationAudioRepositoryPort {

    override fun save(audio: StoryNarrationAudio): StoryNarrationAudio {
        val entity = StoryNarrationAudioEntity(
            id = audio.id,
            translationId = audio.translationId,
            voiceProfile = audio.voiceProfile,
            audioUrl = audio.audioUrl,
            durationSeconds = audio.durationSeconds,
            status = audio.status,
            createdAt = audio.createdAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByTranslationId(translationId: Long): StoryNarrationAudio? =
        jpaRepository.findByTranslationId(translationId)?.toDomain()

    override fun findAllByTranslationIdAndStatus(
        translationId: Long,
        status: NarrationAudioStatus
    ): List<StoryNarrationAudio> =
        jpaRepository.findAllByTranslationIdAndStatus(translationId, status).map { it.toDomain() }

    override fun findByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String): StoryNarrationAudio? =
        jpaRepository.findByTranslationIdAndVoiceProfile(translationId, voiceProfile)?.toDomain()

    override fun existsByTranslationIdAndStatus(
        translationId: Long,
        status: com.araro.domain.narration.NarrationAudioStatus
    ): Boolean = jpaRepository.existsByTranslationIdAndStatus(translationId, status)

    override fun existsByTranslationIdAndVoiceProfileAndStatus(
        translationId: Long,
        voiceProfile: String,
        status: com.araro.domain.narration.NarrationAudioStatus
    ): Boolean = jpaRepository.existsByTranslationIdAndVoiceProfileAndStatus(translationId, voiceProfile, status)

    override fun deleteByTranslationId(translationId: Long) {
        jpaRepository.deleteByTranslationId(translationId)
    }
}

private fun StoryNarrationAudioEntity.toDomain() = StoryNarrationAudio(
    id = id,
    translationId = translationId,
    voiceProfile = voiceProfile,
    audioUrl = audioUrl,
    durationSeconds = durationSeconds,
    status = status,
    createdAt = createdAt
)
