package com.tamixa.infrastructure.voice

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.voice.HeyGenVoiceTtsPort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * HeyGen voice TTS: uses POST /v2/voices/{voice_id}/preview to synthesize text with a HeyGen voice.
 * Voice must be created in HeyGen app (Instant Voice Cloning); set heygen_voice_id on the voice profile via admin PATCH.
 * Uses same HEYGEN_API_KEY as avatar video.
 */
@Component
@ConditionalOnExpression("!'\${app.avatar-video.heygen-api-key:}'.trim().isEmpty()")
class HeyGenVoiceTtsAdapter(
    private val restTemplate: RestTemplate,
    private val appProperties: AppProperties
) : HeyGenVoiceTtsPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    private val apiKey: String get() = appProperties.avatarVideo.heygenApiKey.trim()
    private val baseUrl: String = "https://api.heygen.com"

    override fun synthesize(text: String, voiceId: String, language: String): ByteArray? {
        if (apiKey.isEmpty() || text.isBlank() || voiceId.isBlank()) return null
        val headers = HttpHeaders().apply {
            set("X-Api-Key", apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        val body = mapOf(
            "text" to text,
            "voice_id" to voiceId,
            "text_type" to "plain"
        )
        return try {
            val response = restTemplate.exchange(
                URI("$baseUrl/v2/voices/$voiceId/preview"),
                HttpMethod.POST,
                HttpEntity(body, headers),
                String::class.java
            )
            if (!response.statusCode.is2xxSuccessful || response.body.isNullOrBlank()) {
                log.warn("HeyGen voice preview failed: status={} body={}", response.statusCode, response.body?.take(200))
                return null
            }
            val node = objectMapper.readTree(response.body)
            val audioUrl = node.get("data")?.get("audio_url")?.asText()
                ?: node.get("audio_url")?.asText()
            if (audioUrl.isNullOrBlank()) {
                log.warn("HeyGen voice preview response missing audio_url: {}", response.body?.take(300))
                return null
            }
            val audioResponse = restTemplate.exchange(
                URI(audioUrl),
                HttpMethod.GET,
                null,
                ByteArray::class.java
            )
            if (audioResponse.statusCode.is2xxSuccessful && audioResponse.body != null && audioResponse.body!!.isNotEmpty()) {
                log.debug("HeyGen TTS synthesized bytes={} voiceId={}", audioResponse.body!!.size, voiceId.take(8))
                audioResponse.body
            } else {
                log.warn("HeyGen TTS failed to fetch audio from url: status={}", audioResponse.statusCode)
                null
            }
        } catch (e: Exception) {
            log.error("HeyGen voice preview failed voiceId={} error={}", voiceId.take(8), e.message, e)
            null
        }
    }
}
