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
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Gemini [generateContent](https://ai.google.dev/api/rest/v1beta/models.generateContent) over REST.
 * Supports Google AI (`generativelanguage.googleapis.com`) and Vertex publisher path (`aiplatform.googleapis.com`),
 * selected via [com.tamixa.infrastructure.config.AppProperties.LlmProperties.GeminiLlmProperties.apiUrlStyle].
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
        val url = GeminiUrlBuilder.generateContentUrl(base, model, keyParam, g.apiUrlStyle)

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

        val response = try {
            circuitBreaker.executeSupplier {
                restTemplate.postForObject(url, entity, GeminiGenerateResponse::class.java)
                    ?: throw GeminiApiException("Empty response from Gemini")
            }
        } catch (e: HttpStatusCodeException) {
            throw geminiExceptionFromHttpError(e)
        } catch (e: GeminiApiException) {
            throw e
        } catch (e: Exception) {
            log.warn("Gemini request failed: {}", e.message)
            throw GeminiApiException("Gemini request failed: ${e.message}", e)
        }
        response.error?.let { err ->
            val msg = err.message ?: err.status ?: "Gemini API error"
            log.warn("Gemini API error: code={} status={} message={}", err.code, err.status, msg)
            val hint = geminiTransientOrCapacityHint(msg)
            throw GeminiApiException(if (hint.isNotBlank()) "$msg $hint" else msg)
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

    private fun geminiExceptionFromHttpError(e: HttpStatusCodeException): GeminiApiException {
        val bodyMsg = try {
            val body = e.responseBodyAsString
            if (body.isBlank()) "" else objectMapper.readTree(body).path("error").path("message").asText("").trim()
        } catch (_: Exception) {
            ""
        }
        val base = bodyMsg.ifBlank { e.message ?: e.statusCode.toString() }
        log.warn("Gemini HTTP {}: {}", e.statusCode.value(), base)
        val hint = geminiHttpHint(e.statusCode.value(), base)
        val message = if (hint.isNotBlank()) "$base $hint" else base
        return GeminiApiException(message, e)
    }

    /**
     * Google sometimes returns capacity / overload copy in the error message (any HTTP status).
     */
    private fun geminiTransientOrCapacityHint(apiMessage: String): String {
        val m = apiMessage.lowercase()
        return when {
            m.contains("high demand") ||
                m.contains("resource exhausted") ||
                m.contains("resource_exhausted") ||
                (m.contains("unavailable") && m.contains("try again")) ->
                "[Temporary capacity on Google's side — retry in 1–5 minutes. If it keeps happening, try another GEMINI_MODEL or AI_LLM_PROVIDER=openai.]"
            else -> ""
        }
    }

    private fun geminiHttpHint(httpStatus: Int, apiMessage: String): String {
        geminiTransientOrCapacityHint(apiMessage).takeIf { it.isNotBlank() }?.let { return it }
        val m = apiMessage.lowercase()
        return when {
            httpStatus == 403 && (
                m.contains("generative language api has not been used") ||
                    m.contains("generativelanguage.googleapis.com/overview") ||
                    (m.contains("generative language api") && m.contains("disabled"))
                ) ->
                "[This is not a bad API key: enable Generative Language API on the GCP project shown in Google's message (open the console link they included). Cloud Text-to-Speech is a separate API — enabling TTS does not enable Gemini. After Enable, wait 2–5 minutes and retry.]"
            httpStatus == 403 && (m.contains("denied access") || m.contains("permission_denied") || m.contains("permission denied")) ->
                "[Fix: Use a key from https://aistudio.google.com/apikey — enable Generative Language API on that Google Cloud project, " +
                    "ensure the key has no HTTP referrer restriction for server-side use (or add your server IP), " +
                    "and contact Google Cloud support if the project was suspended or policy-blocked. " +
                    "Alternatively set AI_LLM_PROVIDER=openai with OPENAI_API_KEY.]"
            httpStatus == 403 ->
                "[Gemini 403: check API key, Generative Language API enablement, and key restrictions in Google Cloud Console.]"
            httpStatus == 404 && (m.contains("not available") || m.contains("not found")) ->
                "[Update GEMINI_MODEL to a current model; see https://ai.google.dev/gemini-api/docs/models ]"
            httpStatus == 429 ->
                "[Rate limited or busy: wait 1–5 minutes and retry, or reduce parallel LLM calls.]"
            httpStatus == 503 || httpStatus == 502 ->
                "[Temporary Gemini outage or overload — retry shortly, or try AI_LLM_PROVIDER=openai.]"
            else -> ""
        }
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
