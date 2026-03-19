package com.tamixa.infrastructure.avatar

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.Base64

/**
 * D-ID Talks API client (V2 photo avatar): image + audio URL → talking-head video.
 * See https://docs.d-id.com/docs/v2-photo-avatar-quickstart
 * Create talk with source_url + script.type=audio + audio_url, then poll until status=done, result_url.
 */
@Component
@Conditional(DidAvatarVideoCondition::class)
class DidAvatarVideoClient(
    @Qualifier("didRestTemplate") private val restTemplate: RestTemplate,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val props get() = appProperties.avatarVideo
    private val objectMapper = ObjectMapper()

    private fun baseUrl(): String = props.didBaseUrl.trim().removeSuffix("/")

    private fun authHeader(): String {
        val key = props.didApiKey.trim()
        val encoded = Base64.getEncoder().encodeToString("$key:".toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    /**
     * Create a talk: photo (source_url) + cloned-voice audio (audio_url). Returns talk id for polling.
     */
    fun createTalk(sourceImageUrl: String, audioUrl: String): String? {
        if (props.didApiKey.isBlank()) {
            log.warn("D-ID API key not configured")
            return null
        }
        val body = mapOf(
            "source_url" to sourceImageUrl,
            "script" to mapOf(
                "type" to "audio",
                "audio_url" to audioUrl
            )
        )
        val headers = HttpHeaders().apply {
            set("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
        }
        return try {
            val response = restTemplate.exchange(
                "${baseUrl()}/talks",
                HttpMethod.POST,
                HttpEntity(objectMapper.writeValueAsString(body), headers),
                String::class.java
            )
            if (!response.statusCode.is2xxSuccessful || response.body == null) {
                log.warn("D-ID create talk failed: status={}", response.statusCode)
                return null
            }
            val node = objectMapper.readTree(response.body)
            val id = node.get("id")?.asText()
            if (id != null) {
                log.info("D-ID talk created id={}", id)
                id
            } else {
                log.warn("D-ID create talk: no id in response")
                null
            }
        } catch (e: Exception) {
            log.error("D-ID createTalk failed: {}", e.message, e)
            null
        }
    }

    /**
     * Get talk status. Returns Done(resultUrl) when status=done, Pending otherwise, Error on failure.
     */
    fun getTalkStatus(talkId: String): DidTalkResult {
        if (props.didApiKey.isBlank()) return DidTalkResult.NotConfigured
        val headers = HttpHeaders().apply {
            set("Authorization", authHeader())
        }
        return try {
            val response = restTemplate.exchange(
                "${baseUrl()}/talks/$talkId",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                String::class.java
            )
            if (!response.statusCode.is2xxSuccessful || response.body == null) {
                return DidTalkResult.Error("API error: ${response.statusCode}")
            }
            val node = objectMapper.readTree(response.body)
            val status = node.get("status")?.asText() ?: "unknown"
            when (status) {
                "done" -> {
                    val resultUrl = node.get("result_url")?.asText()
                    if (resultUrl != null) DidTalkResult.Done(resultUrl)
                    else DidTalkResult.Error("No result_url in response")
                }
                "error", "failed" -> {
                    val msg = node.get("error")?.get("message")?.asText()
                        ?: node.get("message")?.asText() ?: "Unknown error"
                    DidTalkResult.Error(msg)
                }
                else -> DidTalkResult.Pending
            }
        } catch (e: Exception) {
            log.warn("D-ID getTalkStatus(id={}) failed: {}", talkId, e.message)
            DidTalkResult.Error(e.message ?: "Unknown error")
        }
    }

    sealed class DidTalkResult {
        data object Pending : DidTalkResult()
        data class Done(val resultUrl: String) : DidTalkResult()
        data class Error(val message: String) : DidTalkResult()
        data object NotConfigured : DidTalkResult()
    }
}
