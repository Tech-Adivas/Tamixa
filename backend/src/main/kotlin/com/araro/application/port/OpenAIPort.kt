package com.araro.application.port

import com.araro.application.story.ModerationResult
import com.araro.application.story.StructuredStoryPayload

/**
 * Token usage as returned by the OpenAI API (for observability and cost tracking).
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Result of structured story generation: payload and usage.
 */
data class StructuredStoryResult(
    val payload: StructuredStoryPayload,
    val usage: TokenUsage?
)

interface OpenAIPort {

    /**
     * Generate story text from a prompt. Kept for backward compatibility.
     */
    fun generateStory(prompt: String): String

    /**
     * Chat completion (system + user). Used for conversation summarization.
     * Returns raw text response; no JSON schema.
     */
    fun completeChat(systemPrompt: String, userMessage: String, maxTokens: Int = 512): String

    /**
     * Generate a structured story (title, moral, story_text, estimated_duration).
     * Uses fallback prompt on retry if provided. Returns token usage for observability.
     */
    fun generateStructuredStory(
        systemPrompt: String,
        userPrompt: String,
        fallbackUserPrompt: String? = null,
        maxTokens: Int = 1024
    ): StructuredStoryResult

    /**
     * Returns true if the text passes moderation (safe). False if flagged.
     */
    fun isContentSafe(text: String): Boolean

    /**
     * Full moderation result with categories (violence, political proxy, etc.) for guardrails.
     */
    fun getModerationResult(text: String): ModerationResult
}
