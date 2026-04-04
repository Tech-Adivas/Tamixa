package com.tamixa.infrastructure.gemini

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.TokenUsage
import com.tamixa.infrastructure.config.AppProperties
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Google AI Gemini [generateContent](https://ai.google.dev/api/rest/v1beta/models.generateContent) over REST.
 * Shared by [GeminiLlmClient], [NarrationGeminiAdapter], and [GeminiTranslationClient].
 */
@Component
@Conditional(GeminiAnyUsageCondition::class)
class GeminiApiClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker: CircuitBreaker = circuitBreakerRegistry.circuitBreaker("gemini")

    private val g get() = appProperties.llm.gemini

    /**
     * @param modelOverride when non-blank, used instead of [AppProperties.LlmProperties.GeminiLlmProperties.model]
     */
    fun generateContent(
        systemInstruction: String?,
        userText: String,
        maxOutputTokens: Int,
        temperature: Double? = null,
        responseMimeType: String? = null,
        modelOverride: String? = null,
    ): Pair<String, TokenUsage?> {
        val apiKey = g.apiKey.trim()
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Gemini API key is not configured. Set GEMINI_API_KEY.")
        }
        val base = g.baseUrl.trimEnd('/')
        val model = (modelOverride?.trim()?.takeIf { it.isNotEmpty() } ?: g.model.trim())
            .removePrefix("models/")
        val keyParam = URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
        val url = "$base/v1beta/models/$model:generateContent?key=$keyParam"

        val generationConfig = GeminiGenerationConfig(
            maxOutputTokens = maxOutputTokens.coerceIn(1, 8192),
            temperature = temperature,
            responseMimeType = responseMimeType,
        )
        val systemBlock = systemInstruction?.trim()?.takeIf { it.isNotEmpty() }?.let {
            GeminiContentBlock(parts = listOf(GeminiPart(text = it)))
        }
        val body = GeminiGenerateRequest(
            systemInstruction = systemBlock,
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = userText))),
            ),
            generationConfig = generationConfig,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val json = objectMapper.writeValueAsString(body)
        val entity = HttpEntity(json, headers)

        val response = circuitBreaker.executeSupplier {
            restTemplate.postForObject(url, entity, GeminiGenerateResponse::class.java)
                ?: throw GeminiApiException("Empty response from Gemini")
        }
        response.error?.let { err ->
            val msg = err.message ?: err.status ?: "Gemini API error"
            log.warn("Gemini API error: code={} status={} message={}", err.code, err.status, msg)
            throw GeminiApiException(msg)
        }
        val text = response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("")?.trim()
        if (text.isNullOrEmpty()) {
            log.warn("Gemini returned no text (candidates empty or blocked)")
            throw GeminiApiException("No text in Gemini response")
        }
        val usage = response.usageMetadata?.let { u ->
            val prompt = u.promptTokenCount ?: 0
            val completion = u.candidatesTokenCount ?: 0
            val total = u.totalTokenCount ?: (prompt + completion)
            TokenUsage(promptTokens = prompt, completionTokens = completion, totalTokens = total)
        }
        return text to usage
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class GeminiGenerateRequest(
        @JsonProperty("systemInstruction") val systemInstruction: GeminiContentBlock? = null,
        val contents: List<GeminiContent>,
        @JsonProperty("generationConfig") val generationConfig: GeminiGenerationConfig,
    )

    private data class GeminiContentBlock(val parts: List<GeminiPart>)

    private data class GeminiContent(
        val role: String,
        val parts: List<GeminiPart>,
    )

    private data class GeminiPart(val text: String)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class GeminiGenerationConfig(
        @JsonProperty("maxOutputTokens") val maxOutputTokens: Int,
        val temperature: Double? = null,
        @JsonProperty("responseMimeType") val responseMimeType: String? = null,
    )

    private data class GeminiGenerateResponse(
        val candidates: List<GeminiCandidate>?,
        @JsonProperty("usageMetadata") val usageMetadata: GeminiUsageMetadata?,
        val error: GeminiApiErrorBody?,
    )

    private data class GeminiCandidate(val content: GeminiCandidateContent?)

    private data class GeminiCandidateContent(
        val parts: List<GeminiPart>?,
        val role: String? = null,
    )

    private data class GeminiUsageMetadata(
        @JsonProperty("promptTokenCount") val promptTokenCount: Int?,
        @JsonProperty("candidatesTokenCount") val candidatesTokenCount: Int?,
        @JsonProperty("totalTokenCount") val totalTokenCount: Int?,
    )

    private data class GeminiApiErrorBody(
        val code: Int? = null,
        val message: String? = null,
        val status: String? = null,
    )
}
