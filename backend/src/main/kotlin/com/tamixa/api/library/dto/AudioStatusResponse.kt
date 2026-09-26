package com.tamixa.api.library.dto

import java.time.Instant

/**
 * Response DTO for audio generation status query.
 * Contains audio status for each language with metadata.
 * Used by admin dashboard for polling audio generation progress.
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 */
data class AudioStatusResponse(
    val storyId: Long,
    val overallStatus: String,
    val languageAudioStatuses: List<LanguageAudioStatus>
) {
    /**
     * Audio status for a single language.
     */
    data class LanguageAudioStatus(
        val language: String,
        val status: String,
        val displayName: String,
        val audioUrl: String? = null,
        val durationSeconds: Int? = null,
        val fileSizeBytes: Long? = null,
        val generatedAt: Instant? = null,
        val error: String? = null,
        val hasAudio: Boolean = false,
        val canRetry: Boolean = false,
        val truncationWarning: Boolean = false
    )
    
    companion object {
        /**
         * Determines overall audio status from individual language statuses.
         * 
         * Logic:
         * - ALL_READY: All languages have READY audio
         * - GENERATING: At least one language is PENDING or PROCESSING
         * - FAILED: At least one language is FAILED and none are GENERATING
         * - PARTIAL: Some languages have audio, some don't
         * - NONE: No languages have audio yet
         */
        fun determineOverallStatus(languageStatuses: List<LanguageAudioStatus>): String {
            if (languageStatuses.isEmpty()) return "NONE"
            
            val readyCount = languageStatuses.count { it.status == "READY" }
            val failedCount = languageStatuses.count { it.status == "FAILED" }
            val generatingCount = languageStatuses.count { it.status in listOf("PENDING", "PROCESSING") }
            
            return when {
                readyCount == languageStatuses.size -> "ALL_READY"
                generatingCount > 0 -> "GENERATING"
                failedCount > 0 && readyCount == 0 -> "FAILED"
                readyCount > 0 -> "PARTIAL"
                else -> "NONE"
            }
        }
    }
}
