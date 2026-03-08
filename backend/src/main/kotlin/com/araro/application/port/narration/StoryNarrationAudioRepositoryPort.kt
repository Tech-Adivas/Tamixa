package com.araro.application.port.narration

import com.araro.domain.narration.NarrationAudioStatus
import com.araro.domain.narration.StoryNarrationAudio

interface StoryNarrationAudioRepositoryPort {

    fun save(audio: StoryNarrationAudio): StoryNarrationAudio

    fun findByTranslationId(translationId: Long): StoryNarrationAudio?

    fun findAllByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): List<StoryNarrationAudio>

    fun findByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String): StoryNarrationAudio?

    fun existsByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): Boolean

    /** Prevents duplicate generation per voice. */
    fun existsByTranslationIdAndVoiceProfileAndStatus(
        translationId: Long,
        voiceProfile: String,
        status: NarrationAudioStatus
    ): Boolean

    fun deleteByTranslationId(translationId: Long)
}
