package com.araro.infrastructure.persistence

import com.araro.domain.narration.NarrationAudioStatus
import org.springframework.data.jpa.repository.JpaRepository

interface StoryNarrationAudioJpaRepository : JpaRepository<StoryNarrationAudioEntity, Long> {

    fun findByTranslationId(translationId: Long): StoryNarrationAudioEntity?

    fun findAllByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): List<StoryNarrationAudioEntity>

    fun findByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String): StoryNarrationAudioEntity?

    fun existsByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): Boolean

    /** Prevents duplicate generation: true if READY audio exists for this (translation, voice) pair. */
    fun existsByTranslationIdAndVoiceProfileAndStatus(
        translationId: Long,
        voiceProfile: String,
        status: NarrationAudioStatus
    ): Boolean

    fun deleteByTranslationId(translationId: Long)
}
