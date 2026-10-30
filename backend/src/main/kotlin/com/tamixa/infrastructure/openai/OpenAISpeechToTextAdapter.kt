package com.tamixa.infrastructure.openai

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.SpeechToTextPort
import com.tamixa.infrastructure.http.ApiErrorExtractor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

/**
 * OpenAI transcription (POST /v1/audio/transcriptions). The audio is sent from memory and never written to disk;
 * the transcript is returned to the caller and not logged.
 */
@Component
class OpenAISpeechToTextAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.voice-answer.transcription-model:whisper-1}") private val model: String,
) : SpeechToTextPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun transcribe(audio: ByteArray, fileName: String, language: String, hint: String?): String? {
        if (apiKey.isBlank()) {
            log.warn("Voice answer transcription skipped: OPENAI_API_KEY is blank")
            return null
        }
        val file = object : ByteArrayResource(audio) {
            override fun getFilename(): String = fileName
        }
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("file", file)
            add("model", model)
            add("response_format", "json")
            language.trim().lowercase().take(5).takeIf { it.isNotBlank() }?.let { add("language", it) }
            hint?.trim()?.take(MAX_HINT_CHARS)?.takeIf { it.isNotBlank() }?.let { add("prompt", it) }
        }
        val headers = HttpHeaders().apply {
            setBearerAuth(apiKey)
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        return try {
            val started = System.currentTimeMillis()
            val response = restTemplate.postForEntity("$baseUrl/v1/audio/transcriptions", HttpEntity(body, headers), String::class.java)
            val text = response.body?.let { objectMapper.readTree(it).path("text").asText("") }?.trim()
            log.info(
                "Voice answer transcribed model={} lang={} audioBytes={} chars={} ms={}",
                model, language, audio.size, text?.length ?: 0, System.currentTimeMillis() - started
            )
            text
        } catch (e: Exception) {
            log.warn("Voice answer transcription failed model={} lang={}: {}", model, language, ApiErrorExtractor.extract(e, objectMapper))
            null
        }
    }

    private companion object {
        const val MAX_HINT_CHARS = 800
    }
}
