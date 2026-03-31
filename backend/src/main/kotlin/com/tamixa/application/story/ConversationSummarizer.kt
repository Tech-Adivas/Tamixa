package com.tamixa.application.story

import com.tamixa.application.port.OpenAIPort
import com.tamixa.infrastructure.openai.OpenAIException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Extracts structured prompt hints (emotion, preferences) from parent conversation
 * messages. Used to tune story generation via prompt modification.
 */
data class ConversationSummary(
    val emotionMode: String?,
    val customPrompt: String?
)

@Component
class ConversationSummarizer(
    private val openAI: OpenAIPort,
    private val objectMapper: ObjectMapper,
    private val storyModeration: StoryModerationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val systemPrompt = """
You are a helper that extracts story customization hints from a parent's conversation about their child.
Given a list of messages (feelings, preferences, context), output valid JSON only:
{"emotionMode": "CALM"|"SOOTHING"|"ADVENTUROUS"|null, "customPrompt": "concise summary of preferences"}
- emotionMode: Use CALM/SOOTHING if parent mentions bedtime, anxious, tired, calming down, sleepy. Use ADVENTUROUS if energetic, daytime, excitement. Use null if unclear.
- customPrompt: Brief summary (max 150 chars) of themes, characters, settings, child's mood. Child-safe only. Empty string if nothing relevant.
Rules: Output only the JSON object. No markdown, no explanation.
""".trimIndent()

    /**
     * Summarize conversation messages into prompt-tuning hints.
     * Returns null if messages empty, API fails, or parsing fails.
     */
    fun summarize(messages: List<String>): ConversationSummary? {
        if (messages.isEmpty()) return null
        val combined = messages.joinToString("\n") { "- $it" }
        val userPrompt = "Conversation messages:\n$combined"
        val response = try {
            openAI.completeChat(systemPrompt, userPrompt, maxTokens = 256)
        } catch (e: OpenAIException) {
            log.debug("Conversation summary API failed: {}", e.message)
            return null
        }
        if (response.isBlank()) return null

        val sessionId = UUID.randomUUID().toString()
        val rawMod = openAI.getModerationResult(response)
        if (!rawMod.safe) {
            log.warn("Conversation summary raw LLM output failed moderation sessionId={}", sessionId)
            return null
        }

        return try {
            val parsed = parseResponse(response) ?: return null
            sanitizeParsedSummary(parsed, sessionId)
        } catch (e: Exception) {
            log.warn("Conversation summary parse failed: {}", e.message)
            val fallbackText = messages.joinToString(" ").take(200).trim()
            if (fallbackText.isBlank()) return null
            try {
                storyModeration.moderateBeforeSave(
                    fallbackText,
                    ModerationContext("conversation-summary-fallback-$sessionId", "en", 8)
                )
            } catch (ex: ContentModerationException) {
                log.warn("Conversation summary fallback rejected by guardrails sessionId={}", sessionId)
                return null
            }
            ConversationSummary(emotionMode = null, customPrompt = fallbackText)
        }
    }

    private fun sanitizeParsedSummary(summary: ConversationSummary, sessionId: String): ConversationSummary {
        val cp = summary.customPrompt ?: return summary
        try {
            storyModeration.moderateBeforeSave(
                cp,
                ModerationContext("conversation-summary-parsed-$sessionId", "en", 8)
            )
        } catch (e: ContentModerationException) {
            log.warn("Conversation summary customPrompt rejected by guardrails sessionId={}", sessionId)
            return ConversationSummary(summary.emotionMode, null)
        }
        return summary
    }

    private fun parseResponse(response: String): ConversationSummary? {
        val trimmed = response.trim().removeSurrounding("```json", "```").trim()
        val tree: JsonNode = objectMapper.readTree(trimmed) ?: return null
        val emotionNode = tree.path("emotionMode")
        val emotionMode = when {
            emotionNode.isNull || emotionNode.isMissingNode -> null
            else -> {
                val s = emotionNode.asText("").uppercase()
                if (s in setOf("CALM", "SOOTHING", "ADVENTUROUS")) s else null
            }
        }
        val customPrompt = tree.path("customPrompt").asText("").trim().take(200).ifBlank { null }
        return ConversationSummary(emotionMode = emotionMode, customPrompt = customPrompt)
    }
}
