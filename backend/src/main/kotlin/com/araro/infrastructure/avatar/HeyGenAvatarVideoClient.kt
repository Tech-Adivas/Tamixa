package com.araro.infrastructure.avatar

import com.araro.infrastructure.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * HeyGen API client for talking-head video from photo + audio.
 * Flow: upload talking photo → create video (audio-driven) → poll status → get video_url
 */
@Component
@ConditionalOnProperty(name = ["app.avatar-video.provider"], havingValue = "heygen")
class HeyGenAvatarVideoClient(
    private val restTemplate: RestTemplate,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val props get() = appProperties.avatarVideo
    private val objectMapper = ObjectMapper()

    companion object {
        private const val API_BASE = "https://api.heygen.com"
        private const val UPLOAD_BASE = "https://upload.heygen.com"
    }

    /**
     * Upload image as talking photo. Returns talking_photo_id for video creation.
     */
    fun uploadTalkingPhoto(imageBytes: ByteArray, contentType: String): String? {
        if (props.heygenApiKey.isBlank()) {
            log.warn("HeyGen API key not configured")
            return null
        }
        val headers = HttpHeaders().apply {
            set("X-Api-Key", props.heygenApiKey)
            setContentType(MediaType.parseMediaType(contentType))
        }
        return try {
            val response = restTemplate.exchange(
                "$UPLOAD_BASE/v1/talking_photo",
                HttpMethod.POST,
                HttpEntity(imageBytes, headers),
                String::class.java
            )
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val node = objectMapper.readTree(response.body)
                val code = node.get("code")?.asInt()
                if (code == 100) {
                    val id = node.get("data")?.get("talking_photo_id")?.asText()
                    log.info("HeyGen talking photo created id={}", id)
                    id
                } else {
                    log.warn("HeyGen talking photo failed: {}", node.get("message")?.asText())
                    null
                }
            } else null
        } catch (e: Exception) {
            log.error("HeyGen uploadTalkingPhoto failed: {}", e.message, e)
            null
        }
    }

    /**
     * Create video: talking photo + audio URL (audio-driven lip sync).
     * Returns video_id for status polling.
     */
    fun createVideo(talkingPhotoId: String, audioUrl: String): String? {
        if (props.heygenApiKey.isBlank()) return null
        val body = mapOf(
            "video_inputs" to listOf(
                mapOf(
                    "character" to mapOf(
                        "type" to "talking_photo",
                        "talking_photo_id" to talkingPhotoId
                    ),
                    "voice" to mapOf(
                        "type" to "audio",
                        "audio_url" to audioUrl
                    ),
                    "background" to mapOf(
                        "type" to "color",
                        "value" to "#FAFAFA"
                    )
                )
            )
        )
        val headers = HttpHeaders().apply {
            set("X-Api-Key", props.heygenApiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        return try {
            val response = restTemplate.exchange(
                "$API_BASE/v2/video/generate",
                HttpMethod.POST,
                HttpEntity(objectMapper.writeValueAsString(body), headers),
                String::class.java
            )
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val node = objectMapper.readTree(response.body)
                val error = node.get("error")?.asText()
                if (error != null) {
                    log.warn("HeyGen createVideo error: {}", error)
                    return null
                }
                val videoId = node.get("data")?.get("video_id")?.asText()
                log.info("HeyGen video created id={}", videoId)
                videoId
            } else null
        } catch (e: Exception) {
            log.error("HeyGen createVideo failed: {}", e.message, e)
            null
        }
    }

    /**
     * Get video status. Returns Succeeded(videoUrl) when completed.
     */
    fun getVideoStatus(videoId: String): AvatarVideoResult {
        if (props.heygenApiKey.isBlank()) return AvatarVideoResult.NotConfigured
        val headers = HttpHeaders().apply {
            set("X-Api-Key", props.heygenApiKey)
        }
        return try {
            val response = restTemplate.exchange(
                "$API_BASE/v1/video_status.get?video_id=$videoId",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                String::class.java
            )
            if (!response.statusCode.is2xxSuccessful || response.body == null) {
                return AvatarVideoResult.Error("API error: ${response.statusCode}")
            }
            val node = objectMapper.readTree(response.body)
            val code = node.get("code")?.asInt()
            if (code != 100) {
                return AvatarVideoResult.Error(node.get("message")?.asText() ?: "Unknown error")
            }
            val data = node.get("data") ?: return AvatarVideoResult.Error("No data")
            val status = data.get("status")?.asText() ?: "unknown"
            when (status) {
                "completed" -> {
                    val videoUrl = data.get("video_url")?.asText()
                    if (videoUrl != null) AvatarVideoResult.Succeeded(videoUrl)
                    else AvatarVideoResult.Error("No video_url in response")
                }
                "failed" -> {
                    val err = data.get("error")
                    val msg = err?.get("message")?.asText() ?: err?.get("detail")?.asText()
                        ?: "Unknown error"
                    AvatarVideoResult.Error(msg)
                }
                else -> AvatarVideoResult.Pending
            }
        } catch (e: Exception) {
            log.warn("HeyGen getVideoStatus(id={}) failed: {}", videoId, e.message)
            AvatarVideoResult.Error(e.message ?: "Unknown error")
        }
    }

    sealed class AvatarVideoResult {
        data object Pending : AvatarVideoResult()
        data class Succeeded(val videoUrl: String) : AvatarVideoResult()
        data class Error(val message: String) : AvatarVideoResult()
        data object NotConfigured : AvatarVideoResult()
    }
}
