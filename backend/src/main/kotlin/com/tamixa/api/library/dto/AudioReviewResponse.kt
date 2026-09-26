package com.tamixa.api.library.dto

import java.time.Instant

/**
 * Response DTO for audio review operations.
 * Contains result of approve/reject operation for audio.
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 * See: .kiro/specs/tamixa-premium-ux-overhaul/design.md Task 10
 */
data class AudioReviewResponse(
    val storyId: Long,
    val language: String? = null,
    val success: Boolean,
    val message: String,
    val status: String? = null,
    val approvedAt: Instant? = null,
    val languagesApproved: List<String>? = null,
    val languagesPending: List<String>? = null
)
