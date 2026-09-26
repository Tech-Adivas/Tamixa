package com.tamixa.api.library.dto

import java.time.Instant

/**
 * Response DTO for audio generation operations.
 * Contains status and results for each language processed.
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 */
data class AudioGenerationResponse(
    val storyId: Long,
    val success: Boolean,
    val message: String,
    val languageResults: List<LanguageAudioResult>,
    val totalDurationMs: Long = 0
) {
    /**
     * Audio generation result for a single language.
     */
    data class LanguageAudioResult(
        val language: String,
        val success: Boolean,
        val status: String,
        val audioUrl: String? = null,
        val durationSeconds: Int? = null,
        val error: String? = null,
        val generatedAt: Instant? = null
    )
}
