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
 * OpenAI Text-to-Speech API adapter.
 * Uses POST /v1/audio/speech with model tts-1-hd for high-quality narration.
 * Strips SSML to plain text (OpenAI TTS does not support SSML).
 * For input longer than MAX_INPUT_CHARS, chunks at sentence/word boundaries and concatenates MP3 output.
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
    @org.springframework.beans.factory.annotation.Value("\${app.narration.openai-tts-speed:1.0}") private val speed: Double
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
        val voice = resolveVoice(voiceProfile)
        aiApiMetrics?.recordOpenaiTts(1, plainText.length)
        val effectiveSpeed = speed.coerceIn(0.25, 4.0)
        val chunks = chunkTextForTts(plainText, MAX_INPUT_CHARS)
        if (chunks.size > 1) {
            log.info("OpenAI TTS: synthesizing {} chunks ({} chars total) for full content", chunks.size, plainText.length)
        }
        val allBytes = mutableListOf<ByteArray>()
        for ((i, chunk) in chunks.withIndex()) {
            val bytes = synthesizeOneChunk(chunk, voice, effectiveSpeed)
            if (bytes != null && bytes.isNotEmpty()) {
                allBytes.add(bytes)
            } else {
                log.warn("OpenAI TTS returned empty audio for chunk {}/{}", i + 1, chunks.size)
            }
        }
        if (allBytes.isEmpty()) {
            log.warn("OpenAI TTS produced no audio")
            return null
        }
        val combined = allBytes.reduce { a, b -> a + b }
        log.debug("OpenAI TTS success: {} chunks, {} bytes total for lang={} voice={}", allBytes.size, combined.size, language, voice)
        return combined
    }

    /** Synthesize a single chunk (under MAX_INPUT_CHARS). */
    private fun synthesizeOneChunk(text: String, voice: String, effectiveSpeed: Double): ByteArray? {
        if (text.isBlank()) return null
        return try {
            val request = mapOf(
                "model" to ttsModel,
                "input" to text,
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
            response.body
        } catch (e: Exception) {
            val msg = ApiErrorExtractor.extract(e, objectMapper)
            log.warn("OpenAI TTS chunk failed: {}", msg, e)
            throw RuntimeException("OpenAI TTS: $msg", e)
        }
    }

    /** Split text into chunks under maxChars, preferring sentence then word boundaries. */
    private fun chunkTextForTts(text: String, maxChars: Int): List<String> {
        val trimmed = text.trim()
        if (trimmed.length <= maxChars) return listOf(trimmed)
        val result = mutableListOf<String>()
        var start = 0
        while (start < trimmed.length) {
            var end = (start + maxChars).coerceAtMost(trimmed.length)
            if (end < trimmed.length) {
                val segment = trimmed.substring(start, end)
                val lastSentence = segment.lastIndexOf('.')
                val lastNewline = segment.lastIndexOf('\n')
                val lastSpace = segment.lastIndexOf(' ')
                val segmentLen = segment.length
                val breakAt = when {
                    lastSentence > segmentLen / 2 -> lastSentence + 1
                    lastNewline > segmentLen / 2 -> lastNewline + 1
                    lastSpace > 0 -> lastSpace + 1
                    else -> segmentLen
                }.coerceAtLeast(1)
                end = start + breakAt
            }
            val chunk = trimmed.substring(start, end).trim()
            if (chunk.isNotBlank()) result.add(chunk)
            start = end
        }
        return result
    }

    private fun ssmlToPlainText(ssml: String): String {
        val plain = ssml
            .replace(Regex("<break[^>]*/>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
        val cleaned = NarrationTextUtils.stripRemainingMarkers(plain)
        return cleaned.ifBlank { plain }.ifBlank { ssml }
    }

    private fun resolveVoice(voiceProfile: String): String {
        val normalized = voiceProfile.trim().lowercase()
        return VOICE_MAP[normalized]
            ?: if (normalized in OPENAI_VOICES) normalized
            else "alloy"
    }
}
