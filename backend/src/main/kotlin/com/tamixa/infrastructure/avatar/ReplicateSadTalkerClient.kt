package com.tamixa.infrastructure.avatar

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
import org.springframework.web.client.HttpClientErrorException

/**
 * Replicate API client for SadTalker (talking-head video generation).
 * API: https://replicate.com/cjwbw/sadtalker/api
 * Input: source_image (HTTP/data URL), driven_audio (HTTP/data URL), use_enhancer, etc.
 * Output: single URI string when status=succeeded (schema: type string, format uri).
 */
@Component
@Conditional(ReplicateSadTalkerCondition::class)
class ReplicateSadTalkerClient(
    @Qualifier("replicateRestTemplate") private val restTemplate: RestTemplate,
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
        // Input schema per https://replicate.com/cjwbw/sadtalker/versions/.../api
        // still_mode=true: fewer head motions (calmer avatar)
        // expression_scale=0.75: gentler lip movement (smaller value = slower/smaller mouth motion)
        val body = mapOf(
            "version" to props.replicateModelVersion,
            "input" to mapOf(
                "source_image" to sourceImageUrl,
                "driven_audio" to drivenAudioUrl,
                "use_enhancer" to true,
                "preprocess" to "crop",
                "still_mode" to true,
                "expression_scale" to 0.75
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
            } else {
                log.warn("Replicate createPrediction non-2xx status={} body={}", response.statusCode, response.body?.take(500))
                null
            }
        } catch (e: Exception) {
            log.error("Replicate createPrediction failed: {}", e.message, e)
            when (e) {
                is HttpClientErrorException -> {
                    val status = e.statusCode.value()
                    val body = e.responseBodyAsString
                    if (status == 402 && (body.contains("Insufficient credit") || body.contains("insufficient credit"))) {
                        throw RuntimeException("Replicate: insufficient credit. Add credit at https://replicate.com/account/billing then try again.")
                    }
                    val detail = try {
                        val node = objectMapper.readTree(body)
                        node.get("detail")?.asText() ?: node.get("title")?.asText() ?: e.message
                    } catch (parseEx: Exception) {
                        log.debug("Replicate error body parse failed: {}", parseEx.message)
                        e.message
                    }
                    throw RuntimeException("Replicate API error ($status): ${detail?.take(200) ?: e.message}")
                }
                else -> return null
            }
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
                val errMsg = "API error: ${response.statusCode}"
                log.warn("Replicate getPrediction(id={}) API error: {}", predictionId, errMsg)
                return ReplicatePredictionResult.Error(errMsg)
            }
            val node = objectMapper.readTree(response.body)
            val status = node.get("status")?.asText() ?: "unknown"
            when (status) {
                "succeeded" -> {
                    // Per SadTalker API schema, output is a single string (URI). Handle string/array/object for robustness.
                    val output = node.get("output")
                    val videoUrl = when {
                        output?.isTextual == true -> output.asText()
                        output?.isArray == true && output.size() > 0 -> output.get(0)?.takeIf { it.isTextual }?.asText()
                        output?.isObject == true -> output.get("video")?.asText()
                            ?: output.fields().asSequence().firstOrNull()?.value?.takeIf { it.isTextual }?.asText()
                        else -> null
                    }
                    if (videoUrl != null && videoUrl.isNotBlank()) ReplicatePredictionResult.Succeeded(videoUrl)
                    else {
                        log.warn("Replicate getPrediction(id={}) succeeded but no video URL in output", predictionId)
                        ReplicatePredictionResult.Error("No video URL in output")
                    }
                }
                "failed" -> {
                    val err = node.get("error")?.asText() ?: "Unknown error"
                    log.warn("Replicate getPrediction(id={}) failed: {}", predictionId, err)
                    ReplicatePredictionResult.Error(err)
                }
                "canceled" -> ReplicatePredictionResult.Error("Prediction canceled")
                else -> ReplicatePredictionResult.Pending
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unknown error"
            log.warn("Replicate getPrediction(id={}) failed: {}", predictionId, e.message, e)
            ReplicatePredictionResult.Error(errMsg)
        }
    }

    sealed class ReplicatePredictionResult {
        data object Pending : ReplicatePredictionResult()
        data class Succeeded(val videoUrl: String) : ReplicatePredictionResult()
        data class Error(val message: String) : ReplicatePredictionResult()
        data object NotConfigured : ReplicatePredictionResult()
    }
}
