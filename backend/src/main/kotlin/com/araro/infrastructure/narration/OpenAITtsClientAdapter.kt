package com.araro.infrastructure.narration

import com.araro.application.port.narration.TtsClientPort
import com.araro.infrastructure.http.ApiErrorExtractor
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
 * OpenAI Text-to-Speech API adapter.
 * Uses POST /v1/audio/speech with model tts-1-hd for high-quality narration.
 * Strips SSML to plain text (OpenAI TTS does not support SSML).
 */
@Component
@Primary
@ConditionalOnProperty(name = ["app.narration.tts-provider"], havingValue = "openai")
class OpenAITtsClientAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Value("\${app.openai.api-key:}") private val apiKey: String,
    @org.springframework.beans.factory.annotation.Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.openai-tts-model:tts-1-hd}") private val ttsModel: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.openai-tts-speed:0.95}") private val speed: Double
) : TtsClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_INPUT_CHARS = 4096
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
        val plainText = ssmlToPlainText(ssml)
        if (plainText.isBlank()) {
            log.warn("OpenAI TTS: SSML yielded empty text after stripping")
            return null
        }
        val chunk = plainText.take(MAX_INPUT_CHARS)
        if (chunk.length < plainText.length) {
            log.debug("OpenAI TTS: truncated input from {} to {} chars", plainText.length, MAX_INPUT_CHARS)
        }
        val voice = resolveVoice(voiceProfile)
        val effectiveSpeed = speed.coerceIn(0.25, 4.0)
        return try {
            val request = mapOf(
                "model" to ttsModel,
                "input" to chunk,
                "voice" to voice,
                "response_format" to "mp3",
                "speed" to effectiveSpeed
            )
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
            val url = "$baseUrl/v1/audio/speech"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)
            val bytes = response.body
            if (bytes != null && bytes.isNotEmpty()) {
                log.debug("OpenAI TTS success: {} bytes for lang={} voice={}", bytes.size, language, voice)
                bytes
            } else {
                log.warn("OpenAI TTS returned empty response")
                null
            }
        } catch (e: Exception) {
            val msg = ApiErrorExtractor.extract(e, objectMapper)
            log.warn("OpenAI TTS failed: {}", msg, e)
            throw RuntimeException("OpenAI TTS: $msg", e)
        }
    }

    private fun ssmlToPlainText(ssml: String): String {
        return ssml
            .replace(Regex("<break[^>]*/>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { ssml }
    }

    private fun resolveVoice(voiceProfile: String): String {
        val normalized = voiceProfile.trim().lowercase()
        return VOICE_MAP[normalized]
            ?: if (normalized in OPENAI_VOICES) normalized
            else "alloy"
    }
}
