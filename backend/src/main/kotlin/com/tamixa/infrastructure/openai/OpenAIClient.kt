package com.tamixa.infrastructure.openai

import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.StructuredStoryResult
import com.tamixa.application.port.TokenUsage
import com.tamixa.application.story.ModerationResult
import com.tamixa.application.story.ModerationCategories
import com.tamixa.application.story.StructuredStoryPayload
import com.tamixa.infrastructure.observability.ApplicationMetrics
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class OpenAIClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val metrics: ApplicationMetrics,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.openai.model:gpt-4o-mini}") private val model: String,
    @Value("\${app.openai.max-tokens:1024}") private val defaultMaxTokens: Int,
    @Value("\${app.openai.moderation-required:false}") private val moderationRequired: Boolean
) : OpenAIPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker: CircuitBreaker = circuitBreakerRegistry.circuitBreaker("openai")

    /** Max 2 retries (3 attempts total), exponential backoff. */
    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [OpenAIException::class, IllegalStateException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    override fun generateStory(prompt: String, maxTokens: Int): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY environment variable.")
        }
        val effectiveMax = maxTokens.coerceIn(256, 4096)
        val request = ChatRequest(
            model = model,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            maxTokens = effectiveMax
        )
        val response = circuitBreaker.executeSupplier { postChat(request) }
        val content = response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw OpenAIException("No content in OpenAI response")
        return content
    }

    override fun completeChat(systemPrompt: String, userMessage: String, maxTokens: Int): String {
        if (apiKey.isBlank()) return ""
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userMessage)
        )
        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = maxTokens.coerceIn(128, 1024)
        )
        return try {
            val response = circuitBreaker.executeSupplier { postChat(request) }
            response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
        } catch (e: Exception) {
            log.warn("Chat completion failed: {}", e.message)
            throw OpenAIException("Chat completion failed: ${e.message}", e)
        }
    }

    override fun generateStructuredStory(
        systemPrompt: String,
        userPrompt: String,
        fallbackUserPrompt: String?,
        maxTokens: Int
    ): StructuredStoryResult {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY environment variable.")
        }
        val effectiveMax = maxTokens.coerceIn(256, 2048)
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )
        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = effectiveMax,
            responseFormat = mapOf("type" to "json_object")
        )
        return try {
            val response = circuitBreaker.executeSupplier { postChat(request) }
            val content = response.choices?.firstOrNull()?.message?.content?.trim()
                ?: throw OpenAIException("No content in OpenAI response")
            val usage = response.usage?.let { TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens) }
            val payload = parseStructuredPayload(content)
            StructuredStoryResult(payload, usage)
        } catch (e: Exception) {
            if (e is OpenAIException) throw e
            if (fallbackUserPrompt != null) {
                log.warn("Structured story parse failed, retrying with fallback prompt: {}", e.message)
                metrics.recordFallbackUsage()
                retryWithFallback(systemPrompt, fallbackUserPrompt, effectiveMax)
            } else {
                throw OpenAIException("Failed to generate structured story: ${e.message}")
            }
        }
    }

    private fun retryWithFallback(systemPrompt: String, fallbackUserPrompt: String, maxTokens: Int): StructuredStoryResult {
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = fallbackUserPrompt)
        )
        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = maxTokens,
            responseFormat = mapOf("type" to "json_object")
        )
        val response = circuitBreaker.executeSupplier { postChat(request) }
        val content = response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw OpenAIException("No content in OpenAI response (fallback)")
        val usage = response.usage?.let { TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens) }
        val payload = parseStructuredPayload(content)
        return StructuredStoryResult(payload, usage)
    }

    /**
     * Parses and validates structured story JSON. Rejects if structure invalid.
     * Required fields: title, moral, story_text, estimated_duration_seconds.
     */
    private fun parseStructuredPayload(content: String): StructuredStoryPayload {
        val trimmed = content.trim().removeSurrounding("```json", "```").trim()
        val tree: JsonNode = objectMapper.readTree(trimmed)
            ?: throw OpenAIException("Invalid JSON from model")
        val title = tree.path("title").asText("").trim()
        if (title.isBlank()) throw OpenAIException("Missing or empty title in response")
        val moral = tree.path("moral").asText("").trim()
        val storyText = tree.path("story_text").asText("").ifBlank { tree.path("storyText").asText("") }.trim()
        if (storyText.isBlank()) throw OpenAIException("Missing or empty story_text in response")
        val secondsNode = tree.path("estimated_duration_seconds")
        val estimatedDurationSeconds = when {
            secondsNode.isNumber -> secondsNode.asInt().coerceIn(1, 3600)
            else -> {
                val minutes = tree.path("estimated_duration").asDouble(0.0).coerceIn(0.5, 30.0)
                (minutes * 60).toInt()
            }
        }
        return StructuredStoryPayload(
            title = title,
            moral = moral,
            storyText = storyText,
            estimatedDurationSeconds = estimatedDurationSeconds
        )
    }

    private fun postChat(request: ChatRequest): ChatResponse {
        val headers = HttpHeaders().apply {
            setBearerAuth(apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(request, headers)
        val url = "$baseUrl/v1/chat/completions"
        return restTemplate.postForObject(url, entity, ChatResponse::class.java)
            ?: throw OpenAIException("Empty response from OpenAI")
    }

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [OpenAIException::class, IllegalStateException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    override fun isContentSafe(text: String): Boolean = getModerationResult(text).safe

    override fun getModerationResult(text: String): ModerationResult {
        if (apiKey.isBlank()) {
            if (moderationRequired) {
                log.warn("OpenAI moderation required but API key is blank; failing closed for child safety")
                return ModerationResult(safe = false)
            }
            return ModerationResult(safe = true)
        }
        val request = ModerationApiRequest(input = text)
        val headers = HttpHeaders().apply {
            setBearerAuth(apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(request, headers)
        val url = "$baseUrl/v1/moderations"
        return try {
            val response = restTemplate.postForObject(url, entity, ModerationApiResponse::class.java)
            val result = response?.results?.firstOrNull()
            if (result == null) {
                log.warn("Moderation API returned no result; failing closed (rejecting content) for safety")
                return ModerationResult(safe = false)
            }
            val categories = result.categories
            val violence = categories?.violence == true || categories?.violenceGraphic == true
            val sexual = categories?.sexual == true || categories?.sexualMinors == true
            val hate = categories?.hate == true || categories?.hateThreatening == true
            val selfHarm = categories?.selfHarm == true
            val harassment = categories?.harassment == true
            val safe = !(violence || sexual || hate || selfHarm || harassment)
            ModerationResult(
                safe = safe,
                categories = ModerationCategories(
                    hate = hate,
                    hateThreatening = categories?.hateThreatening == true,
                    harassment = harassment,
                    selfHarm = selfHarm,
                    sexual = sexual,
                    sexualMinors = categories?.sexualMinors == true,
                    violence = violence,
                    violenceGraphic = categories?.violenceGraphic == true
                )
            )
        } catch (e: Exception) {
            log.error("Moderation API failed; failing closed (rejecting content) for child safety: {}", e.message, e)
            return ModerationResult(safe = false)
        }
    }

    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @JsonProperty("max_tokens") val maxTokens: Int,
        @JsonProperty("response_format") val responseFormat: Map<String, String>? = null
    )

    private data class ChatMessage(val role: String, val content: String)

    private data class ChatResponse(
        val choices: List<Choice>?,
        val usage: Usage?
    )

    private data class Choice(val message: Message?)

    private data class Message(val content: String?)

    private data class Usage(
        @JsonProperty("prompt_tokens") val promptTokens: Int,
        @JsonProperty("completion_tokens") val completionTokens: Int,
        @JsonProperty("total_tokens") val totalTokens: Int
    )

    private data class ModerationApiRequest(val input: String)

    private data class ModerationApiResponse(val results: List<ModerationApiResult>?)

    private data class ModerationApiResult(
        val flagged: Boolean,
        val categories: ModerationCategoriesApi?
    )

    private data class ModerationCategoriesApi(
        val hate: Boolean? = null,
        @JsonProperty("hate/threatening") val hateThreatening: Boolean? = null,
        val harassment: Boolean? = null,
        @JsonProperty("self-harm") val selfHarm: Boolean? = null,
        val sexual: Boolean? = null,
        @JsonProperty("sexual/minors") val sexualMinors: Boolean? = null,
        val violence: Boolean? = null,
        @JsonProperty("violence/graphic") val violenceGraphic: Boolean? = null
    )
}

class OpenAIException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
