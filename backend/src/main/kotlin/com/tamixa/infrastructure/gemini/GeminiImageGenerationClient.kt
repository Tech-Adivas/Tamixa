package com.tamixa.infrastructure.gemini

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.story.CoverIllustrationPrompts
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * [ImageGenerationPort] via Gemini API native image generation ([generateContent] with IMAGE modality).
 *
 * @see [Google image generation docs](https://ai.google.dev/gemini-api/docs/image-generation)
 */
@Component
@ConditionalOnProperty(name = ["app.image-generation.provider"], havingValue = "gemini")
class GeminiImageGenerationClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties,
) : ImageGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val g get() = appProperties.llm.gemini
    private val img get() = appProperties.imageGeneration.gemini

    override fun generateImage(prompt: String): ByteArray? {
        val apiKey = g.apiKey.trim()
        if (apiKey.isEmpty()) {
            log.warn("Gemini cover image skipped: GEMINI_API_KEY is blank")
            return null
        }
        val base = g.baseUrl.trimEnd('/')
        val model = img.model.trim().removePrefix("models/")
        if (model.isEmpty()) {
            log.warn("Gemini cover image skipped: image model is blank")
            return null
        }
        val keyParam = URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
        val url = "$base/v1beta/models/$model:generateContent?key=$keyParam"
        val userPrompt = CoverIllustrationPrompts.buildChildSafeCoverPrompt(prompt)
        val aspect = img.aspectRatio.trim().ifBlank { "1:1" }
        val size = img.imageSize.trim().ifBlank { "1K" }
        val imageConfig = if (img.useImageConfig) {
            GeminiImageConfig(aspectRatio = aspect, imageSize = size)
        } else {
            null
        }
        val body = GeminiImageGenerateRequest(
            contents = listOf(
                GeminiImageContent(
                    role = "user",
                    parts = listOf(GeminiImagePart(text = userPrompt)),
                ),
            ),
            generationConfig = GeminiImageGenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE"),
                imageConfig = imageConfig,
            ),
        )
        val json = objectMapper.writeValueAsString(body)
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        return try {
            val entity = HttpEntity(json, headers)
            val response = restTemplate.postForEntity(url, entity, String::class.java)
            val raw = response.body ?: run {
                log.warn("Gemini image generation: empty response body")
                return null
            }
            extractImageBytes(raw)
        } catch (e: HttpClientErrorException) {
            log.warn(
                "Gemini image generation HTTP error: status={} body={}",
                e.statusCode,
                e.responseBodyAsString?.take(500),
            )
            null
        } catch (e: Exception) {
            log.warn("Gemini image generation failed: {} cause={}", e.message, e.cause?.message, e)
            null
        }
    }

    private fun extractImageBytes(responseJson: String): ByteArray? {
        val root = try {
            objectMapper.readTree(responseJson)
        } catch (e: Exception) {
            log.warn("Gemini image response not JSON: {}", e.message)
            return null
        }
        root.get("error")?.let { err ->
            val msg = err.get("message")?.asText() ?: err.toString()
            log.warn("Gemini image API error: {}", msg.take(500))
            return null
        }
        val candidates = root.get("candidates") ?: return noImageInResponse("no candidates")
        if (!candidates.isArray || candidates.isEmpty) return noImageInResponse("empty candidates")
        for (candidate in candidates) {
            val finish = candidate.get("finishReason")?.asText()?.uppercase()
            if (finish == "SAFETY" || finish == "BLOCKLIST" || finish == "PROHIBITED_CONTENT" || finish == "IMAGE_SAFETY") {
                log.warn("Gemini image blocked: finishReason={}", finish)
                return null
            }
            val parts = candidate.get("content")?.get("parts") ?: continue
            if (!parts.isArray) continue
            for (part in parts) {
                val inline = part.get("inlineData") ?: part.get("inline_data") ?: continue
                val dataB64 = inline.get("data")?.asText()?.trim()
                if (!dataB64.isNullOrEmpty()) {
                    return try {
                        Base64.getDecoder().decode(dataB64)
                    } catch (e: IllegalArgumentException) {
                        log.warn("Gemini image: invalid base64 in response")
                        null
                    }
                }
            }
        }
        return noImageInResponse("no image parts in candidates")
    }

    private fun noImageInResponse(hint: String): ByteArray? {
        log.warn("Gemini image generation returned no image bytes ({})", hint)
        return null
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class GeminiImageGenerateRequest(
        val contents: List<GeminiImageContent>,
        @JsonProperty("generationConfig") val generationConfig: GeminiImageGenerationConfig,
    )

    private data class GeminiImageContent(
        val role: String,
        val parts: List<GeminiImagePart>,
    )

    private data class GeminiImagePart(val text: String)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class GeminiImageGenerationConfig(
        @JsonProperty("responseModalities") val responseModalities: List<String>,
        @JsonProperty("imageConfig") val imageConfig: GeminiImageConfig? = null,
    )

    private data class GeminiImageConfig(
        @JsonProperty("aspectRatio") val aspectRatio: String,
        @JsonProperty("imageSize") val imageSize: String,
    )
}
