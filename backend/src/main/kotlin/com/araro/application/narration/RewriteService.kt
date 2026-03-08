package com.araro.application.narration

import com.araro.domain.narration.ToneMode

/**
 * Conversational transformation: rewrites translated story text for TTS narration.
 * Step 2 of deterministic two-step LLM pipeline (after TranslationService).
 * Implemented by NarrationFormatterService.
 */
interface RewriteService {

    /**
     * Transform story text into conversational narration script.
     * @param storyText Translated content (from step 1)
     * @param age Child age for vocabulary/pace
     * @param toneMode CALM or EXPRESSIVE
     * @param language Target language code
     * @return Formatted script with token usage for cost tracking
     */
    fun rewrite(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String
    ): RewriteResult
}

data class RewriteResult(
    val scriptText: String,
    val promptTokens: Int,
    val completionTokens: Int
)

/** Adapter: NarrationFormatterService implements RewriteService for pipeline clarity. */
fun NarrationFormatResult.toRewriteResult(): RewriteResult =
    RewriteResult(scriptText = scriptText, promptTokens = promptTokens, completionTokens = completionTokens)
