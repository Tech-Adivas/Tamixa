package com.tamixa.infrastructure.narration

import com.tamixa.application.narration.AudioStorageService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

/**
 * No-op storage when S3 is not configured. Returns placeholder path.
 * S3NarrationAudioStorageAdapter takes precedence when app.storage.type=s3.
 */
@Component
@ConditionalOnMissingBean(AudioStorageService::class)
class NoOpNarrationAudioStorageAdapter : AudioStorageService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun uploadNarrationAudio(
        storyId: Long,
        language: String,
        voiceProfile: String,
        mp3Bytes: ByteArray
    ): String {
        val slug = if (voiceProfile == "default") "v1" else voiceProfile.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "v1" }
        log.warn("NoOp storage: S3 not configured. Returning placeholder path for storyId={} lang={} voice={}", storyId, language, voiceProfile)
        return "stories/$storyId/$language/$slug.mp3"
    }
}
