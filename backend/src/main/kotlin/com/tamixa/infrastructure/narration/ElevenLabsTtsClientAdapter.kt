package com.tamixa.infrastructure.narration

import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.port.narration.TtsClientPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.ElevenLabsVoiceSettings
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Story narrator (non-cloned voices) on ElevenLabs: NARRATION_TTS_PROVIDER=elevenlabs.
 *
 * Narrator voices are ElevenLabs voice ids — e.g. native Tamil storytellers from the ElevenLabs voice library, which
 * can be used by id without adding them to the account. `default` uses [defaultVoiceId]; `calm` uses [calmVoiceId]
 * (falls back to the default); other profiles use the default. Speech goes through the same ElevenLabs client as
 * cloned voices (model, parts with surrounding text so the voice stays the same across a story) but with the
 * narrator's own voice settings (app.narration.elevenlabs-*), so tuning the narrator doesn't flatten parents' clones.
 * SSML is turned into paragraph text, so the voice pauses between narration segments.
 */
@Component
@Primary
@ConditionalOnProperty(name = ["app.narration.tts-provider"], havingValue = "elevenlabs")
class ElevenLabsTtsClientAdapter(
    @Autowired(required = false) private val elevenLabs: ElevenLabsVoiceCloningPort?,
    @Value("\${app.narration.elevenlabs-voice-id:}") defaultVoiceIdRaw: String,
    @Value("\${app.narration.elevenlabs-calm-voice-id:}") calmVoiceIdRaw: String,
    @Value("\${app.narration.elevenlabs-stability:0.7}") stability: Double = 0.7,
    @Value("\${app.narration.elevenlabs-similarity-boost:0.9}") similarityBoost: Double = 0.9,
    @Value("\${app.narration.elevenlabs-speed:0.9}") speed: Double = 0.9,
) : TtsClientPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val defaultVoiceId = defaultVoiceIdRaw.trim()
    private val calmVoiceId = calmVoiceIdRaw.trim().ifBlank { defaultVoiceId }

    /** Narrator voice settings, separate from cloned voices (a steady, calm storyteller pace suits library voices). */
    internal val settings = ElevenLabsVoiceSettings(stability = stability, similarityBoost = similarityBoost, speed = speed)

    init {
        when {
            elevenLabs == null -> log.warn("Narration TTS provider is elevenlabs but ELEVENLABS_API_KEY is not set; narration will fail")
            defaultVoiceId.isBlank() -> log.warn("Narration TTS provider is elevenlabs but NARRATION_ELEVENLABS_VOICE_ID is not set; narration will fail")
            else -> log.info("Narration TTS: ElevenLabs narrator voice={} calmVoice={}", defaultVoiceId.take(8), calmVoiceId.take(8))
        }
    }

    override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? {
        val client = elevenLabs ?: return null
        val voiceId = voiceIdFor(voiceProfile)
        if (voiceId.isBlank()) return null
        val text = NarrationTextUtils.ssmlToSpeechText(ssml)
        if (text.isBlank()) {
            log.warn("ElevenLabs narration: SSML yielded empty text")
            return null
        }
        log.info("ElevenLabs narration lang={} voiceProfile={} voice={} chars={}", language, voiceProfile, voiceId.take(8), text.length)
        return client.synthesize(text, voiceId, language, settings)
    }

    internal fun voiceIdFor(voiceProfile: String): String =
        if (voiceProfile.trim().equals("calm", ignoreCase = true)) calmVoiceId else defaultVoiceId
}
