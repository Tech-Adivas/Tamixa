package com.tamixa.infrastructure.persistence

import com.tamixa.domain.narration.NarrationAudioStatus
import org.springframework.data.jpa.repository.JpaRepository

interface StoryNarrationAudioJpaRepository : JpaRepository<StoryNarrationAudioEntity, Long> {

    fun findByTranslationId(translationId: Long): StoryNarrationAudioEntity?

    fun findAllByTranslationId(translationId: Long): List<StoryNarrationAudioEntity>

    fun findAllByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): List<StoryNarrationAudioEntity>

    fun findByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String): StoryNarrationAudioEntity?

    fun findByTranslationIdInAndVoiceProfile(translationIds: List<Long>, voiceProfile: String): List<StoryNarrationAudioEntity>

    fun existsByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): Boolean

    /** Prevents duplicate generation: true if READY audio exists for this (translation, voice) pair. */
    fun existsByTranslationIdAndVoiceProfileAndStatus(
        translationId: Long,
        voiceProfile: String,
        status: NarrationAudioStatus
    ): Boolean

    fun deleteByTranslationId(translationId: Long)

    /** Delete single narration entry for a voice profile (e.g. cloned:1) so it can be regenerated. */
    fun deleteByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String)
}
