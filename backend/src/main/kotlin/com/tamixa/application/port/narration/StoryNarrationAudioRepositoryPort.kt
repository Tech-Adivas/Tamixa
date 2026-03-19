package com.tamixa.application.port.narration

import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationAudio

interface StoryNarrationAudioRepositoryPort {

    fun save(audio: StoryNarrationAudio): StoryNarrationAudio

    fun findByTranslationId(translationId: Long): StoryNarrationAudio?

    fun findAllByTranslationId(translationId: Long): List<StoryNarrationAudio>

    fun findAllByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): List<StoryNarrationAudio>

    fun findByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String): StoryNarrationAudio?

    fun findByTranslationIdInAndVoiceProfile(translationIds: List<Long>, voiceProfile: String): List<StoryNarrationAudio>

    fun existsByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): Boolean

    /** Prevents duplicate generation per voice. */
    fun existsByTranslationIdAndVoiceProfileAndStatus(
        translationId: Long,
        voiceProfile: String,
        status: NarrationAudioStatus
    ): Boolean

    fun deleteByTranslationId(translationId: Long)

    /** Delete narration for (translation, voiceProfile) so next request regenerates (e.g. after deleting cloned-voice cache). */
    fun deleteByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String)

    /** Find all narration audio (for admin cleanup). */
    fun findAll(): List<StoryNarrationAudio>

    /** Delete all narration audio rows. Call after deleting S3 objects. */
    fun deleteAll()
}
