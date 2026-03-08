package com.araro.application.port.narration

import com.araro.domain.narration.ToneMode

/**
 * LLM abstraction for narration formatting.
 * Implemented by OpenAI or configured provider.
 */
interface NarrationLLMPort {

    /**
     * Generate formatted narration from story text.
     * Prompt built internally; no dynamic injection.
     */
    fun formatNarration(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String,
        maxTokens: Int
    ): NarrationLLMResult
}

data class NarrationLLMResult(
    val formattedText: String,
    val promptTokens: Int,
    val completionTokens: Int
)
