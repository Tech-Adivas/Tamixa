package com.tamixa.infrastructure.llm

import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.StructuredStoryResult
import com.tamixa.application.story.ModerationResult

/**
 * Story LLM with a backup provider: story generation, Regenerate & sync, summaries and moderation.
 * Primary = `AI_LLM_PROVIDER`, backup = `AI_LLM_FALLBACK_PROVIDER` (wired in [ProviderFallbackConfig]).
 *
 * A blank text answer counts as a failure, so an unconfigured primary (e.g. empty key) also falls back.
 * A moderation verdict ("flagged") is a valid answer and is never retried on the backup.
 */
class FallbackStoryLlm(
    val primary: OpenAIPort,
    val fallback: OpenAIPort,
    private val runner: ProviderFallbackRunner,
) : OpenAIPort {

    private val nonBlank: (String) -> Boolean = { it.isNotBlank() }

    override fun generateStory(prompt: String, maxTokens: Int): String =
        runner.run(
            "generateStory", nonBlank,
            primary = { primary.generateStory(prompt, maxTokens) },
            fallback = { fallback.generateStory(prompt, maxTokens) },
        )

    override fun completeChat(systemPrompt: String, userMessage: String, maxTokens: Int): String =
        runner.run(
            "completeChat", nonBlank,
            primary = { primary.completeChat(systemPrompt, userMessage, maxTokens) },
            fallback = { fallback.completeChat(systemPrompt, userMessage, maxTokens) },
        )

    override fun generateStructuredStory(
        systemPrompt: String,
        userPrompt: String,
        fallbackUserPrompt: String?,
        maxTokens: Int,
    ): StructuredStoryResult =
        runner.run(
            "generateStructuredStory",
            primary = { primary.generateStructuredStory(systemPrompt, userPrompt, fallbackUserPrompt, maxTokens) },
            fallback = { fallback.generateStructuredStory(systemPrompt, userPrompt, fallbackUserPrompt, maxTokens) },
        )

    override fun isContentSafe(text: String): Boolean =
        runner.run(
            "isContentSafe",
            primary = { primary.isContentSafe(text) },
            fallback = { fallback.isContentSafe(text) },
        )

    override fun getModerationResult(text: String): ModerationResult =
        runner.run(
            "getModerationResult",
            primary = { primary.getModerationResult(text) },
            fallback = { fallback.getModerationResult(text) },
        )
}
