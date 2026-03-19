package com.tamixa.application.port.narration

import com.tamixa.domain.narration.ToneMode

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

    /**
     * Transform story text using a custom user prompt (admin TTS preview).
     * Output is used as input to TTS. Used for "Regenerate with prompt" and pipeline rewrite.
     */
    fun transformWithCustomPrompt(
        storyText: String,
        customPrompt: String,
        maxTokens: Int
    ): NarrationLLMResult
}

data class NarrationLLMResult(
    val formattedText: String,
    val promptTokens: Int,
    val completionTokens: Int
)
