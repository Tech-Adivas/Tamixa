package com.araro.application.narration

import com.araro.domain.narration.ToneMode

/**
 * Converts raw story text into conversational narration script.
 * Uses LLM for age-appropriate, tone-adjusted formatting.
 */
interface NarrationFormatterService {

    /**
     * Format story text into conversational narration.
     * @param storyText Original translated story content
     * @param age Child age for vocabulary level
     * @param toneMode CALM (bedtime) or EXPRESSIVE
     * @param language BCP-47 code for consistency
     * @return Formatted script text
     */
    fun formatNarration(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String
    ): NarrationFormatResult
}

/**
 * Result of narration formatting with optional token usage.
 */
data class NarrationFormatResult(
    val scriptText: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
)
