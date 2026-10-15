package com.tamixa.infrastructure.openai

import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.story.CoverIllustrationPrompts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
@ConditionalOnProperty(
    name = ["app.image-generation.provider"],
    havingValue = "openai",
    matchIfMissing = true,
)
class DalleImageClient(
    private val restTemplate: RestTemplate,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    // GPT Image models replace the retired DALL·E 3; "dall-e-*" is still accepted for accounts that have it.
    @Value("\${app.openai.image-model:gpt-image-1}") private val imageModel: String
) : ImageGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generateImage(prompt: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("DALL-E skipped: OPENAI_API_KEY is blank")
            return null
        }
        return try {
            val model = imageModel.trim().ifBlank { "gpt-image-1" }
            val isDalle = model.startsWith("dall-e", ignoreCase = true)
            val request = buildMap<String, Any> {
                put("model", model)
                put("prompt", CoverIllustrationPrompts.buildChildSafeCoverPrompt(prompt))
                put("n", 1)
                put("size", "1024x1024")
                if (isDalle) {
                    // DALL·E-only parameters; GPT Image models reject "style" (400 unknown_parameter).
                    put("quality", "standard")
                    put("style", "vivid")
                } else {
                    put("quality", "medium")
                }
            }
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val response = restTemplate.postForObject(
                "$baseUrl/v1/images/generations",
                HttpEntity(request, headers),
                ImageGenResponse::class.java
            )
            val image = response?.data?.firstOrNull() ?: return null
            // GPT Image models return base64 data; DALL·E returns a temporary URL.
            image.b64Json?.takeIf { it.isNotBlank() }?.let { return java.util.Base64.getDecoder().decode(it) }
            val imageUrl = image.url ?: return null
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

    private data class ImageGenResponse(val data: List<ImageData>?)

    private data class ImageData(
        val url: String? = null,
        @com.fasterxml.jackson.annotation.JsonProperty("b64_json") val b64Json: String? = null
    )
}
