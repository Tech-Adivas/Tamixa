package com.araro.infrastructure.openai

import com.araro.application.port.ImageGenerationPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
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
        } catch (e: Exception) {
            log.warn("DALL-E image generation failed: {} cause={}", e.message, e.cause?.message, e)
            null
        }
    }

    private fun buildChildSafePrompt(theme: String): String {
        val safe = theme.take(900).replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
        return "Child-friendly, warm, colorful illustration for a kids story. Animated HD style. No text, words, or letters in the image—pure illustration only. Soft, cartoon-like, no scary elements. Scene: $safe. Safe for ages 3-12."
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
