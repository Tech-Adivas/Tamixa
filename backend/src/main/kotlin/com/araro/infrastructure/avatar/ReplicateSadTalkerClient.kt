package com.araro.infrastructure.avatar

import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Replicate API client for SadTalker (talking-head video generation).
 * Input: source_image URL, driven_audio URL
 * Output: video URL when status=succeeded
 */
@Component
@ConditionalOnProperty(name = ["app.avatar-video.provider"], havingValue = "sadtalker")
class ReplicateSadTalkerClient(
    private val restTemplate: RestTemplate,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val props get() = appProperties.avatarVideo
    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()

    companion object {
        private const val API_BASE = "https://api.replicate.com/v1"
    }

    /**
     * Create prediction. Returns prediction ID for async polling.
     */
    fun createPrediction(sourceImageUrl: String, drivenAudioUrl: String): String? {
        if (props.replicateApiToken.isBlank()) {
            log.warn("Replicate API token not configured")
            return null
        }
        val body = mapOf(
            "version" to props.replicateModelVersion,
            "input" to mapOf(
                "source_image" to sourceImageUrl,
                "driven_audio" to drivenAudioUrl,
                "use_enhancer" to true
            )
        )
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${props.replicateApiToken}")
            contentType = MediaType.APPLICATION_JSON
        }
        return try {
            val response = restTemplate.exchange(
                "$API_BASE/predictions",
                HttpMethod.POST,
                HttpEntity(objectMapper.writeValueAsString(body), headers),
                String::class.java
            )
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val node = objectMapper.readTree(response.body)
                val id = node.get("id")?.asText()
                log.info("Replicate prediction created id={}", id)
                id
            } else null
        } catch (e: Exception) {
            log.error("Replicate createPrediction failed: {}", e.message, e)
            null
        }
    }

    /**
     * Get prediction status. Returns output URL when succeeded, null otherwise.
     */
    fun getPrediction(predictionId: String): ReplicatePredictionResult {
        if (props.replicateApiToken.isBlank()) return ReplicatePredictionResult.NotConfigured
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${props.replicateApiToken}")
        }
        return try {
            val response = restTemplate.exchange(
                "$API_BASE/predictions/$predictionId",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                String::class.java
            )
            if (!response.statusCode.is2xxSuccessful || response.body == null) {
                return ReplicatePredictionResult.Error("API error: ${response.statusCode}")
            }
            val node = objectMapper.readTree(response.body)
            val status = node.get("status")?.asText() ?: "unknown"
            when (status) {
                "succeeded" -> {
                    val output = node.get("output")
                    val videoUrl = when {
                        output?.isTextual == true -> output.asText()
                        output?.isObject == true -> output.get("video")?.asText()
                            ?: output.fields().asSequence().firstOrNull()?.value?.asText()
                        else -> null
                    }
                    if (videoUrl != null) ReplicatePredictionResult.Succeeded(videoUrl)
                    else ReplicatePredictionResult.Error("No video URL in output")
                }
                "failed" -> {
                    val err = node.get("error")?.asText() ?: "Unknown error"
                    ReplicatePredictionResult.Error(err)
                }
                "canceled" -> ReplicatePredictionResult.Error("Prediction canceled")
                else -> ReplicatePredictionResult.Pending
            }
        } catch (e: Exception) {
            log.warn("Replicate getPrediction(id={}) failed: {}", predictionId, e.message)
            ReplicatePredictionResult.Error(e.message ?: "Unknown error")
        }
    }

    sealed class ReplicatePredictionResult {
        data object Pending : ReplicatePredictionResult()
        data class Succeeded(val videoUrl: String) : ReplicatePredictionResult()
        data class Error(val message: String) : ReplicatePredictionResult()
        data object NotConfigured : ReplicatePredictionResult()
    }
}
