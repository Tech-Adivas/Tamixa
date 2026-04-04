package com.tamixa.infrastructure.gemini

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.StructuredStoryResult
import com.tamixa.application.port.TokenUsage
import com.tamixa.application.story.ModerationCategories
import com.tamixa.application.story.ModerationResult
import com.tamixa.application.story.StructuredStoryPayload
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.ApplicationMetrics
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

/**
 * [OpenAIPort] backed by Google Gemini generateContent (JSON mode for structured stories).
 */
@Component
@ConditionalOnProperty(name = ["app.llm.provider"], havingValue = "gemini")
class GeminiLlmClient(
    private val geminiApiClient: GeminiApiClient,
    private val objectMapper: ObjectMapper,
    private val metrics: ApplicationMetrics,
    private val appProperties: AppProperties,
) : OpenAIPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val moderationRequired get() = appProperties.llm.gemini.moderationRequired

    private val moderationSystemPrompt = """
        You are a content safety classifier for a children's story app.
        Output ONLY valid JSON (no markdown) with these boolean keys exactly:
        "hate", "hate_threatening", "harassment", "self_harm", "sexual", "sexual_minors", "violence", "violence_graphic".
        Set true when the user text clearly matches that category; otherwise false. Be strict for sexual, violence, self_harm, and minors.
    """.trimIndent()

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [GeminiApiException::class, IllegalStateException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun generateStory(prompt: String, maxTokens: Int): String {
        val effectiveMax = maxTokens.coerceIn(256, 8192)
        val (text, _) = geminiApiClient.generateContent(
            systemInstruction = null,
            userText = prompt,
            maxOutputTokens = effectiveMax,
            temperature = 0.85,
            responseMimeType = null,
        )
        return text
    }

    override fun completeChat(systemPrompt: String, userMessage: String, maxTokens: Int): String {
        if (appProperties.llm.gemini.apiKey.isBlank()) return ""
        val effectiveMax = maxTokens.coerceIn(128, 8192)
        return try {
            val (text, _) = geminiApiClient.generateContent(
                systemInstruction = systemPrompt,
                userText = userMessage,
                maxOutputTokens = effectiveMax,
                temperature = 0.5,
                responseMimeType = null,
            )
            text
        } catch (e: Exception) {
            log.warn("Gemini chat completion failed: {}", e.message)
            throw GeminiApiException("Chat completion failed: ${e.message}", e)
        }
    }

    override fun generateStructuredStory(
        systemPrompt: String,
        userPrompt: String,
        fallbackUserPrompt: String?,
        maxTokens: Int,
    ): StructuredStoryResult {
        if (appProperties.llm.gemini.apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.")
        }
        val effectiveMax = maxTokens.coerceIn(256, 8192)
        return try {
            val (content, usage) = geminiApiClient.generateContent(
                systemInstruction = systemPrompt,
                userText = userPrompt,
                maxOutputTokens = effectiveMax,
                temperature = 0.75,
                responseMimeType = "application/json",
            )
            val payload = parseStructuredPayload(content)
            StructuredStoryResult(payload, usage)
        } catch (e: Exception) {
            if (e is GeminiApiException) throw e
            if (fallbackUserPrompt != null) {
                log.warn("Structured story parse failed, retrying with fallback prompt: {}", e.message)
                metrics.recordFallbackUsage()
                retryWithFallback(systemPrompt, fallbackUserPrompt, effectiveMax)
            } else {
                throw GeminiApiException("Failed to generate structured story: ${e.message}", e)
            }
        }
    }

    private fun retryWithFallback(
        systemPrompt: String,
        fallbackUserPrompt: String,
        maxTokens: Int,
    ): StructuredStoryResult {
        val (content, usage) = geminiApiClient.generateContent(
            systemInstruction = systemPrompt,
            userText = fallbackUserPrompt,
            maxOutputTokens = maxTokens,
            temperature = 0.75,
            responseMimeType = "application/json",
        )
        val payload = parseStructuredPayload(content)
        return StructuredStoryResult(payload, usage)
    }

    private fun parseStructuredPayload(content: String): StructuredStoryPayload {
        val trimmed = content.trim().removeSurrounding("```json", "```").trim()
            .removeSurrounding("```", "```").trim()
        val tree: JsonNode = objectMapper.readTree(trimmed)
            ?: throw GeminiApiException("Invalid JSON from model")
        val title = tree.path("title").asText("").trim()
        if (title.isBlank()) throw GeminiApiException("Missing or empty title in response")
        val moral = tree.path("moral").asText("").trim()
        val storyText = tree.path("story_text").asText("").ifBlank { tree.path("storyText").asText("") }.trim()
        if (storyText.isBlank()) throw GeminiApiException("Missing or empty story_text in response")
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
            estimatedDurationSeconds = estimatedDurationSeconds,
        )
    }

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [GeminiApiException::class, IllegalStateException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    override fun isContentSafe(text: String): Boolean = getModerationResult(text).safe

    override fun getModerationResult(text: String): ModerationResult {
        if (appProperties.llm.gemini.apiKey.isBlank()) {
            if (moderationRequired) {
                log.warn("Gemini moderation required but API key is blank; failing closed for child safety")
                return ModerationResult(safe = false)
            }
            return ModerationResult(safe = true)
        }
        val sample = text.take(24_000)
        return try {
            val (raw, _) = geminiApiClient.generateContent(
                systemInstruction = moderationSystemPrompt,
                userText = "Classify this text:\n\n$sample",
                maxOutputTokens = 256,
                temperature = 0.0,
                responseMimeType = "application/json",
            )
            parseModerationJson(raw)
        } catch (e: Exception) {
            log.error("Gemini moderation failed; failing closed for child safety: {}", e.message, e)
            ModerationResult(safe = false)
        }
    }

    private fun parseModerationJson(raw: String): ModerationResult {
        val trimmed = raw.trim().removeSurrounding("```json", "```").trim()
            .removeSurrounding("```", "```").trim()
        val tree: JsonNode = try {
            objectMapper.readTree(trimmed)
        } catch (e: Exception) {
            log.warn("Moderation JSON parse failed: {}", e.message)
            return ModerationResult(safe = false)
        } ?: return ModerationResult(safe = false)

        fun bool(vararg keys: String): Boolean {
            for (k in keys) {
                val n = tree.get(k) ?: continue
                if (n.isBoolean) return n.asBoolean()
                if (n.isTextual) return n.asText().equals("true", ignoreCase = true)
            }
            return false
        }

        val hate = bool("hate")
        val hateThreatening = bool("hate_threatening", "hateThreatening", "hate/threatening")
        val harassment = bool("harassment")
        val selfHarm = bool("self_harm", "selfHarm", "self-harm")
        val sexualRaw = bool("sexual")
        val sexualMinorsRaw = bool("sexual_minors", "sexualMinors", "sexual/minors")
        val sexualCombined = sexualRaw || sexualMinorsRaw
        val violenceRaw = bool("violence")
        val violenceGraphicRaw = bool("violence_graphic", "violenceGraphic", "violence/graphic")
        val violenceCombined = violenceRaw || violenceGraphicRaw

        val flagged = hate || hateThreatening || harassment || selfHarm || sexualCombined || violenceCombined
        return ModerationResult(
            safe = !flagged,
            categories = ModerationCategories(
                hate = hate,
                hateThreatening = hateThreatening,
                harassment = harassment,
                selfHarm = selfHarm,
                sexual = sexualCombined,
                sexualMinors = sexualMinorsRaw,
                violence = violenceCombined,
                violenceGraphic = violenceGraphicRaw,
            ),
        )
    }
}
