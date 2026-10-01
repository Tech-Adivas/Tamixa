package com.tamixa.infrastructure.narration

import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.application.port.narration.NarrationLLMResult
import com.tamixa.domain.narration.ToneMode
import com.tamixa.infrastructure.llm.ProviderFallbackRunner

/**
 * Narration rewrite / "Regenerate with prompt" with a backup provider. Blank output counts as a failure.
 * Used by [OpenAiWithGeminiFallbackNarrationLlmAdapter] and [GeminiWithOpenAiFallbackNarrationLlmAdapter].
 */
class FallbackNarrationLlm(
    val primary: NarrationLLMPort,
    val fallback: NarrationLLMPort,
    private val runner: ProviderFallbackRunner,
) : NarrationLLMPort {

    private val nonBlank: (NarrationLLMResult) -> Boolean = { it.formattedText.isNotBlank() }

    override fun formatNarration(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String,
        maxTokens: Int,
    ): NarrationLLMResult =
        runner.run(
            "formatNarration lang=$language", nonBlank,
            primary = { primary.formatNarration(storyText, age, toneMode, language, maxTokens) },
            fallback = { fallback.formatNarration(storyText, age, toneMode, language, maxTokens) },
        )

    override fun transformWithCustomPrompt(
        storyText: String,
        customPrompt: String,
        maxTokens: Int,
    ): NarrationLLMResult =
        runner.run(
            "transformWithCustomPrompt", nonBlank,
            primary = { primary.transformWithCustomPrompt(storyText, customPrompt, maxTokens) },
            fallback = { fallback.transformWithCustomPrompt(storyText, customPrompt, maxTokens) },
        )
}
