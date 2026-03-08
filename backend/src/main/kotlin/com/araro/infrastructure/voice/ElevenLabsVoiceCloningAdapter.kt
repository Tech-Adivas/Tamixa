package com.araro.infrastructure.voice

import com.araro.application.port.voice.ElevenLabsVoiceCloningPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

/**
 * ElevenLabs voice cloning adapter.
 * Add voice: POST /v1/voices/add (multipart)
 * TTS: POST /v1/text-to-speech/{voice_id}
 *
 * Required: ELEVENLABS_API_KEY, app.voice-cloning.enabled=true
 */
@Component
@ConditionalOnProperty(name = ["app.voice-cloning.enabled"], havingValue = "true")
class ElevenLabsVoiceCloningAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.elevenlabs-api-key:}") private val apiKey: String,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.elevenlabs-base-url:https://api.elevenlabs.io}") private val baseUrl: String
) : ElevenLabsVoiceCloningPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun addVoice(audioBytes: ByteArray, fileName: String, name: String): String? {
        if (apiKey.isBlank()) {
            log.warn("ElevenLabs addVoice skipped: API key is blank")
            return null
        }
        return try {
            val headers = HttpHeaders().apply {
                set("xi-api-key", apiKey)
                contentLength = -1
            }
            val fileResource = object : ByteArrayResource(audioBytes) {
                override fun getFilename(): String = fileName
            }
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("name", name)
                add("files", fileResource)
            }
            val entity = HttpEntity(body, headers)
            val url = "$baseUrl/v1/voices/add"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, Map::class.java)
            val voiceId = (response.body?.get("voice_id") as? String)
            if (voiceId != null) {
                log.info("ElevenLabs voice created voiceId={}", voiceId.take(8))
            }
            voiceId
        } catch (e: Exception) {
            log.warn("ElevenLabs addVoice failed: {}", e.message, e)
            null
        }
    }

    override fun synthesize(text: String, voiceId: String, language: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("ElevenLabs synthesize skipped: API key is blank")
            return null
        }
        if (text.isBlank()) return null
        return try {
            val plainText = text.take(10_000)
            val request = mapOf(
                "text" to plainText,
                "model_id" to "eleven_multilingual_v2",
                "language_code" to mapLanguageToElevenLabs(language)
            )
            val headers = HttpHeaders().apply {
                set("xi-api-key", apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
            val url = "$baseUrl/v1/text-to-speech/$voiceId?output_format=mp3_44100_128"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)
            response.body
        } catch (e: Exception) {
            log.warn("ElevenLabs synthesize failed voiceId={}: {}", voiceId.take(8), e.message, e)
            null
        }
    }

    private fun mapLanguageToElevenLabs(lang: String): String {
        val normalized = lang.trim().lowercase().take(5)
        return when (normalized) {
            "ta" -> "ta"
            "hi" -> "hi"
            "te" -> "te"
            "kn" -> "kn"
            "ml" -> "ml"
            "bn" -> "bn"
            "en" -> "en"
            else -> "en"
        }
    }
}
