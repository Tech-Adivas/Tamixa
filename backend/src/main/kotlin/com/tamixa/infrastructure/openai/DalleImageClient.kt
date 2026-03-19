package com.tamixa.infrastructure.openai

import com.tamixa.application.port.ImageGenerationPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class DalleImageClient(
    private val restTemplate: RestTemplate,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String
) : ImageGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generateImage(prompt: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("DALL-E skipped: OPENAI_API_KEY is blank")
            return null
        }
        return try {
            val request = ImageGenRequest(
                model = "dall-e-3",
                prompt = buildChildSafePrompt(prompt),
                n = 1,
                size = "1024x1024",
                quality = "standard",
                style = "vivid"
            )
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val response = restTemplate.postForObject(
                "$baseUrl/v1/images/generations",
                HttpEntity(request, headers),
                ImageGenResponse::class.java
            )
            val imageUrl = response?.data?.firstOrNull()?.url ?: return null
            restTemplate.getForObject(URI(imageUrl), ByteArray::class.java)
        } catch (e: HttpClientErrorException.BadRequest) {
            val responseBody = e.responseBodyAsString
            val isPolicyViolation = responseBody?.contains("content_policy_violation", ignoreCase = true) == true
            if (isPolicyViolation) {
                log.warn(
                    "DALL-E prompt blocked by content policy. Adjust prompt content and retry. status={} body={}",
                    e.statusCode,
                    responseBody
                )
            } else {
                log.warn("DALL-E bad request: status={} body={}", e.statusCode, responseBody)
            }
            null
        } catch (e: Exception) {
            log.warn("DALL-E image generation failed: {} cause={}", e.message, e.cause?.message, e)
            null
        }
    }

    /**
     * Safety/style wrapper for DALL-E that keeps visuals child-safe while preserving story intent.
     * Avoids fixed visual motifs (e.g. temple/kid/gold) unless the story explicitly asks for them.
     */
    private fun buildChildSafePrompt(theme: String): String {
        val safe = theme.take(900).replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
        return (
            "Create a premium storybook cover illustration for Tamixa app. " +
                "Follow the story scene details exactly; do not default to generic temple, child portrait, or gold-tinted composition unless explicitly described. " +
                "Keep it child-safe, calm, and emotionally warm (no violence, gore, fear, weapons, or distress). " +
                "Use a refined, modern storybook style (not cartoonish, not photoreal). " +
                "Color direction: derive colors from the story mood and setting while staying compatible with dark app UI (balanced contrast, no harsh neon, no blown-out whites). " +
                "No text, words, logos, or letters in the image. High-definition square cover. " +
                "Story scene input: $safe."
            )
    }

    private data class ImageGenRequest(
        val model: String,
        val prompt: String,
        val n: Int,
        val size: String,
        val quality: String,
        val style: String
    )

    private data class ImageGenResponse(val data: List<ImageData>?)

    private data class ImageData(val url: String?)
}
