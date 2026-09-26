package com.tamixa.api.library.dto

import jakarta.validation.constraints.Size

/**
 * Request DTO for audio review operations (approve/reject).
 * Used when admin reviews generated audio for a specific language.
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 * See: .kiro/specs/tamixa-premium-ux-overhaul/design.md Task 10
 */
data class AudioReviewRequest(
    /**
     * Optional notes for rejection or approval.
     * Used to provide feedback when rejecting audio.
     */
    @field:Size(max = 2000, message = "Notes must be at most 2000 characters")
    val notes: String? = null,
    
    /**
     * Optional flag to force regeneration after rejection.
     * Defaults to true for reject operations.
     */
    val forceRegenerate: Boolean = true
)
