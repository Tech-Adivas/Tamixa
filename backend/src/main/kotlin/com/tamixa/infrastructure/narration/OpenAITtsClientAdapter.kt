package com.tamixa.infrastructure.narration

import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.port.narration.TtsClientPort
import com.tamixa.infrastructure.http.ApiErrorExtractor
import com.tamixa.infrastructure.observability.AiApiMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * OpenAI Text-to-Speech API adapter: POST /v1/audio/speech with the configured model (tts-1-hd by default).
 * OpenAI TTS does not read SSML, so the SSML is turned into plain text that keeps one paragraph per narration
 * segment (blank line between), where the voice pauses; the SSML's own breaks would otherwise be lost and the story
 * read in one breath. Long text is sent in parts (paragraph / sentence boundaries) and the MP3s are joined; if any
 * part fails the whole narration fails rather than silently skipping a piece of the story.
 *
 * gpt-4o-mini-tts style models also get [instructions] (e.g. a calm, steady storyteller pace); tts-1 / tts-1-hd don't
 * accept them. Their input limit is in tokens (~2000), so they get smaller parts.
 */
@Component
@Primary
@ConditionalOnProperty(name = ["app.narration.tts-provider"], havingValue = "openai")
class OpenAITtsClientAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Autowired(required = false) private val aiApiMetrics: AiApiMetrics?,
    @org.springframework.beans.factory.annotation.Value("\${app.openai.api-key:}") private val apiKey: String,
    @org.springframework.beans.factory.annotation.Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.openai-tts-model:tts-1-hd}") private val ttsModel: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.openai-tts-speed:1.0}") private val speed: Double,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.openai-tts-instructions:}") private val instructions: String = ""
) : TtsClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_INPUT_CHARS = 4096
        /** gpt-4o TTS models take ~2000 tokens; Indic scripts use several tokens per word, so parts stay small. */
        private const val MAX_INPUT_CHARS_GPT4O = 1000
        private val OPENAI_VOICES = setOf("alloy", "ash", "ballad", "coral", "echo", "fable", "onyx", "nova", "sage", "shimmer", "verse", "marin", "cedar")
        private val VOICE_MAP = mapOf(
            "default" to "alloy",
            "calm" to "nova",
            "v1" to "alloy"
        )
    }

    override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("OpenAI TTS skipped: OPENAI_API_KEY is blank")
            return null
        }
        val plainText = NarrationTextUtils.ssmlToSpeechText(ssml)
        if (plainText.isBlank()) {
            log.warn("OpenAI TTS: SSML yielded empty text after stripping")
            return null
        }
        val voice = resolveVoice(voiceProfile)
        aiApiMetrics?.recordOpenaiTts(1, plainText.length)
        val effectiveSpeed = speed.coerceIn(0.25, 4.0)
        val maxChars = if (isGpt4oModel()) MAX_INPUT_CHARS_GPT4O else MAX_INPUT_CHARS
        val chunks = NarrationTextUtils.splitForSpeech(plainText, maxChars)
        if (chunks.size > 1) {
            log.info("OpenAI TTS: synthesizing {} chunks ({} chars total) model={} for full content", chunks.size, plainText.length, ttsModel)
        }
        val allBytes = mutableListOf<ByteArray>()
        for ((i, chunk) in chunks.withIndex()) {
            val bytes = synthesizeOneChunk(chunk, voice, effectiveSpeed)
            if (bytes == null || bytes.isEmpty()) {
                // A missing part would make the story jump ahead; fail so the narration is marked failed and retried.
                log.warn("OpenAI TTS returned empty audio for chunk {}/{}; narration not saved", i + 1, chunks.size)
                return null
            }
            allBytes.add(bytes)
        }
        val combined = Mp3Joiner.join(allBytes)
        log.debug("OpenAI TTS success: {} chunks, {} bytes total for lang={} voice={}", allBytes.size, combined.size, language, voice)
        return combined
    }

    /** Synthesize a single chunk (under MAX_INPUT_CHARS). */
    private fun synthesizeOneChunk(text: String, voice: String, effectiveSpeed: Double): ByteArray? {
        if (text.isBlank()) return null
        return try {
            val request = buildMap<String, Any> {
                put("model", ttsModel)
                put("input", text)
                put("voice", voice)
                put("response_format", "mp3")
                // 1.0 is the API default; only sent when changed.
                if (effectiveSpeed != 1.0) put("speed", effectiveSpeed)
                if (isGpt4oModel() && instructions.isNotBlank()) put("instructions", instructions.trim())
            }
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
            val url = "$baseUrl/v1/audio/speech"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)
            response.body
        } catch (e: Exception) {
            val msg = ApiErrorExtractor.extract(e, objectMapper)
            log.warn("OpenAI TTS chunk failed: {}", msg, e)
            throw RuntimeException("OpenAI TTS: $msg", e)
        }
    }

    /** gpt-4o-mini-tts and later: take [instructions], limit input in tokens. */
    private fun isGpt4oModel(): Boolean = ttsModel.trim().lowercase().startsWith("gpt-4o")

    private fun resolveVoice(voiceProfile: String): String {
        val normalized = voiceProfile.trim().lowercase()
        return VOICE_MAP[normalized]
            ?: if (normalized in OPENAI_VOICES) normalized
            else "alloy"
    }
}
