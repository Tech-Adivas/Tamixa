package com.araro.application.story

import com.araro.application.port.OpenAIPort
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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
    private val objectMapper: ObjectMapper
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
        val response = openAI.completeChat(systemPrompt, userPrompt, maxTokens = 256)
        if (response.isBlank()) return null
        return try {
            parseResponse(response)
        } catch (e: Exception) {
            log.warn("Conversation summary parse failed: {}", e.message)
            // Fallback: concatenate messages as customPrompt, no emotion
            ConversationSummary(
                emotionMode = null,
                customPrompt = messages.joinToString(" ").take(200)
            )
        }
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
