package com.tamixa.infrastructure.narration

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.narration.AlignedWord
import com.tamixa.application.port.narration.SpeechAlignmentPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets

/**
 * ElevenLabs forced alignment: POST /v1/forced-alignment (multipart: file + text) returns per-word start/end times.
 * Works on any finished MP3 (narrator or parent's cloned voice, any TTS provider), including Tamil.
 * Enabled whenever ELEVENLABS_API_KEY is set.
 */
@Component
@ConditionalOnExpression("!'\${app.voice-cloning.eleven-labs-api-key:}'.trim().isEmpty()")
class ElevenLabsForcedAlignmentAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.voice-cloning.eleven-labs-api-key:}") apiKeyRaw: String,
    @Value("\${app.voice-cloning.eleven-labs-base-url:https://api.elevenlabs.io}") private val baseUrl: String
) : SpeechAlignmentPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val apiKey = apiKeyRaw.trim()

    override fun alignWords(audio: ByteArray, transcript: String): List<AlignedWord>? {
        if (audio.isEmpty() || transcript.isBlank()) return null
        val startMs = System.currentTimeMillis()
        return try {
            val filePart = object : ByteArrayResource(audio) {
                override fun getFilename() = "narration.mp3"
            }
            // Explicit UTF-8: multipart string parts otherwise default to ISO-8859-1 and mangle Tamil text.
            val textPart = HttpEntity(
                transcript,
                HttpHeaders().apply { contentType = MediaType("text", "plain", StandardCharsets.UTF_8) }
            )
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("file", filePart)
                add("text", textPart)
            }
            val headers = HttpHeaders().apply {
                set("xi-api-key", apiKey)
                contentType = MediaType.MULTIPART_FORM_DATA
            }
            val json = restTemplate.postForObject(
                "${baseUrl.trimEnd('/')}/v1/forced-alignment",
                HttpEntity(body, headers),
                String::class.java
            ) ?: return null
            val words = parseWords(json)
            log.info(
                "ElevenLabs forced alignment ok words={} audioBytes={} transcriptChars={} in {}ms",
                words.size, audio.size, transcript.length, System.currentTimeMillis() - startMs
            )
            words.ifEmpty { null }
        } catch (e: Exception) {
            log.warn("ElevenLabs forced alignment failed audioBytes={} transcriptChars={}: {}", audio.size, transcript.length, e.message)
            null
        }
    }

    /** The response lists spaces as their own "words"; keep only real words, in order. */
    internal fun parseWords(json: String): List<AlignedWord> =
        objectMapper.readTree(json).path("words").mapNotNull { node ->
            val text = node.path("text").asText("").trim()
            if (text.isEmpty() || !node.has("start") || !node.has("end")) null
            else AlignedWord(text, node.path("start").asDouble(), node.path("end").asDouble())
        }
}
