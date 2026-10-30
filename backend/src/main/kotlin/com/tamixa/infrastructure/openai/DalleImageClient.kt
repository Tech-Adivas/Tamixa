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
    @Value("\${app.openai.image-model:gpt-image-1}") private val imageModel: String,
    @Value("\${app.story-visuals.image-quality:medium}") private val sceneQuality: String = "medium",
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

    /** Reference-image edits take up to ~1.5 min; own client so the shared one's timeout stays short. */
    private val sceneRestTemplate: RestTemplate by lazy {
        org.springframework.boot.web.client.RestTemplateBuilder()
            .setConnectTimeout(java.time.Duration.ofSeconds(15))
            .setReadTimeout(java.time.Duration.ofSeconds(240))
            .build()
    }

    /**
     * Portrait story-scene picture with GPT Image. With [references] it uses /v1/images/edits (the references keep
     * characters consistent); without, /v1/images/generations. DALL·E models can't take references: returns null.
     */
    override fun generateStoryScene(prompt: String, references: List<ByteArray>): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("Story scene skipped: OPENAI_API_KEY is blank")
            return null
        }
        val model = imageModel.trim().ifBlank { "gpt-image-1" }
        if (model.startsWith("dall-e", ignoreCase = true)) {
            log.warn("Story scene skipped: model {} does not support reference images", model)
            return null
        }
        val fullPrompt = "$prompt $STORY_SCENE_SAFETY"
        return try {
            val headers = HttpHeaders().apply { setBearerAuth(apiKey) }
            val response = if (references.isEmpty()) {
                headers.contentType = MediaType.APPLICATION_JSON
                val body = mapOf("model" to model, "prompt" to fullPrompt, "n" to 1, "size" to SCENE_SIZE, "quality" to sceneQuality)
                sceneRestTemplate.postForObject("$baseUrl/v1/images/generations", HttpEntity(body, headers), ImageGenResponse::class.java)
            } else {
                headers.contentType = MediaType.MULTIPART_FORM_DATA
                val body = org.springframework.util.LinkedMultiValueMap<String, Any>().apply {
                    references.forEachIndexed { i, bytes ->
                        add("image[]", object : org.springframework.core.io.ByteArrayResource(bytes) {
                            override fun getFilename() = "reference_$i.png"
                        })
                    }
                    add("model", model)
                    add("prompt", fullPrompt)
                    add("n", "1")
                    add("size", SCENE_SIZE)
                    add("quality", sceneQuality)
                }
                sceneRestTemplate.postForObject("$baseUrl/v1/images/edits", HttpEntity(body, headers), ImageGenResponse::class.java)
            }
            response?.data?.firstOrNull()?.b64Json?.takeIf { it.isNotBlank() }?.let { java.util.Base64.getDecoder().decode(it) }
        } catch (e: HttpClientErrorException) {
            log.warn("Story scene request rejected: status={} body={}", e.statusCode, e.responseBodyAsString.take(400))
            null
        } catch (e: Exception) {
            log.warn("Story scene generation failed: {}", e.message)
            null
        }
    }

    private data class ImageGenResponse(val data: List<ImageData>?)

    private data class ImageData(
        val url: String? = null,
        @com.fasterxml.jackson.annotation.JsonProperty("b64_json") val b64Json: String? = null
    )
}

private const val SCENE_SIZE = "1024x1536"
private const val STORY_SCENE_SAFETY =
    "Child-safe, calm and warm (no violence, fear, weapons or distress). No text, words, letters, labels or logos in the image."
