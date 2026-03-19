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
import java.util.concurrent.ConcurrentHashMap

/**
 * Gooey.AI lip-sync API client (image + audio → talking-head video).
 * Uses the LipsyncTTS endpoint (example_id in query). Set AVATAR_VIDEO_PROVIDER=gooey, GOOEY_API_KEY and GOOEY_RECIPE_ID in .env.
 * Recipe API page e.g. https://gooey.ai/lipsync-maker/hd-lipsync-basic-y0lxo7gn28t4/api/
 */
@Component
@Conditional(GooeyLipSyncCondition::class)
class GooeyLipSyncClient(
    @Qualifier("gooeyRestTemplate") private val restTemplate: RestTemplate,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val props get() = appProperties.avatarVideo
    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()

    private fun baseUrl(): String = props.gooeyBaseUrl.trim().removeSuffix("/")

    /**
     * Start a lip-sync job. Uses LipsyncTTS endpoint with example_id. Returns job ID for polling, or null on failure.
     * HD Lipsync (basic) requires text_prompt; sends input_face (avatar image) and tts_provider (e.g. ELEVEN_LABS).
     */
    fun createJob(imageUrl: String, audioUrl: String, textPrompt: String): String? {
        if (props.gooeyApiKey.isBlank()) {
            log.warn("Gooey API key not configured")
            return null
        }
        if (props.gooeyRecipeId.isBlank()) {
            log.warn("Gooey recipe ID not configured")
            return null
        }
        if (textPrompt.isBlank()) {
            log.warn("Gooey text_prompt is blank; API requires it")
            return null
        }
        val body = buildMap<String, Any> {
            put("text_prompt", textPrompt.trim().take(50_000))
            put("tts_provider", props.gooeyTtsProvider.ifBlank { "ELEVEN_LABS" })
            put("input_face", imageUrl)
        }
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${props.gooeyApiKey}")
            contentType = MediaType.APPLICATION_JSON
        }
        val url = "${baseUrl()}/v2/LipsyncTTS?example_id=${props.gooeyRecipeId}"
        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(objectMapper.writeValueAsString(body), headers),
                String::class.java
            )
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val node = objectMapper.readTree(response.body)
                val id = node.get("id")?.asText() ?: node.get("run_id")?.asText()
                val outputVideo = node.get("output")?.get("output_video")?.asText()
                if (!outputVideo.isNullOrBlank() && id != null) {
                    log.info("Gooey lip-sync completed synchronously id={}", id.take(50))
                    runIdsWithImmediateOutput[id] = outputVideo
                } else {
                    log.info("Gooey lip-sync job created id={}", id?.take(50))
                }
                id
            } else {
                log.warn("Gooey createJob non-2xx status={} body={}", response.statusCode, response.body?.take(500))
                null
            }
        } catch (e: Exception) {
            log.error("Gooey createJob failed: {}", e.message, e)
            null
        }
    }

    private val runIdsWithImmediateOutput = ConcurrentHashMap<String, String>()

    /**
     * Get job status. Returns Succeeded(videoUrl), Pending, or Error.
     * If the create response was synchronous, the video URL is served from the in-memory cache.
     */
    fun getJobStatus(jobId: String): GooeyJobResult {
        if (props.gooeyApiKey.isBlank()) return GooeyJobResult.NotConfigured
        runIdsWithImmediateOutput.remove(jobId)?.let { videoUrl ->
            return GooeyJobResult.Succeeded(videoUrl)
        }
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${props.gooeyApiKey}")
        }
        return try {
            val response = restTemplate.exchange(
                "${baseUrl()}/v2/run/$jobId",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                String::class.java
            )
            if (!response.statusCode.is2xxSuccessful || response.body == null) {
                return GooeyJobResult.Error("API error: ${response.statusCode}")
            }
            val node = objectMapper.readTree(response.body)
            val status = node.get("status")?.asText()?.lowercase() ?: "unknown"
            when (status) {
                "completed", "succeeded", "complete" -> {
                    val output = node.get("output")
                    val videoUrl = when {
                        output == null -> null
                        output.isTextual -> output.asText()
                        output.has("output_video") -> output.get("output_video")?.asText()
                        output.has("output_url") -> output.get("output_url")?.asText()
                        output.has("video_url") -> output.get("video_url")?.asText()
                        else -> node.get("output_url")?.asText()
                    }
                    if (!videoUrl.isNullOrBlank()) GooeyJobResult.Succeeded(videoUrl)
                    else GooeyJobResult.Error("No video URL in output")
                }
                "failed", "error" -> {
                    val err = node.get("error")?.asText() ?: node.get("message")?.asText() ?: "Unknown error"
                    GooeyJobResult.Error(err)
                }
                else -> GooeyJobResult.Pending
            }
        } catch (e: Exception) {
            log.warn("Gooey getJobStatus(id={}) failed: {}", jobId.take(50), e.message)
            GooeyJobResult.Error(e.message ?: "Unknown error")
        }
    }

    sealed class GooeyJobResult {
        data object Pending : GooeyJobResult()
        data class Succeeded(val videoUrl: String) : GooeyJobResult()
        data class Error(val message: String) : GooeyJobResult()
        data object NotConfigured : GooeyJobResult()
    }
}
