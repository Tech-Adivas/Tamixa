package com.tamixa.api.library.dto

/**
 * Response DTO for pipeline status query.
 * Contains status for each language translation.
 * Used by admin dashboard for polling (every 3 seconds).
 */
data class PipelineStatusResponse(
    val storyId: Long,
    val languageStatuses: List<LanguageStatus>
) {
    /**
     * Status for a single language translation.
     */
    data class LanguageStatus(
        val language: String,
        val status: String,
        val displayName: String,
        val isFailed: Boolean,
        val isTerminal: Boolean
    )
}
