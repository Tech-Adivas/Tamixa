package com.tamixa.api.library.dto

/**
 * Response DTO for pipeline trigger operation.
 * Contains overall success status and per-language results.
 */
data class TriggerPipelineResponse(
    val storyId: Long,
    val success: Boolean,
    val message: String,
    val languageResults: List<LanguageResult>,
    val totalDurationMs: Long
) {
    /**
     * Result for a single language translation.
     */
    data class LanguageResult(
        val language: String,
        val success: Boolean,
        val error: String? = null,
        val translationId: Long? = null
    )
}
