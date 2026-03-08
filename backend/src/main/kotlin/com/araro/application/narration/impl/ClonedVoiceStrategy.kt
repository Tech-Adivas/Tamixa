package com.araro.application.narration.impl

import com.araro.application.narration.VoiceSynthesisStrategy
import com.araro.application.port.VoiceRepositoryPort
import com.araro.application.port.voice.ElevenLabsVoiceCloningPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Synthesizes speech using parent's cloned voice (ElevenLabs).
 * When voiceProfile is "cloned:{voiceProfileId}", looks up the voice profile,
 * gets elevenlabs_voice_id, and synthesizes with ElevenLabs TTS.
 */
@Component
class ClonedVoiceStrategy(
    private val voiceRepository: VoiceRepositoryPort,
    @Autowired(required = false) private val elevenLabs: ElevenLabsVoiceCloningPort?
) : VoiceSynthesisStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray? {
        if (!voiceProfile.startsWith(CLONED_PREFIX)) return null
        val idStr = voiceProfile.removePrefix(CLONED_PREFIX).trim()
        val profileId = idStr.toLongOrNull() ?: return null
        val profile = voiceRepository.findById(profileId) ?: return null
        val voiceId = profile.elevenlabsVoiceId
        if (voiceId.isNullOrBlank()) {
            log.warn("Voice profile {} has no elevenlabs_voice_id", profileId)
            return null
        }
        if (elevenLabs == null) {
            log.warn("ElevenLabs voice cloning not configured")
            return null
        }
        val plainText = ssml.replace(Regex("<[^>]+>"), " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace(Regex("\\s+"), " ").trim()
        if (plainText.isBlank()) return null
        return elevenLabs.synthesize(plainText, voiceId, language)
    }

    companion object {
        private const val CLONED_PREFIX = "cloned:"
    }
}
