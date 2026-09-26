package com.tamixa.api.library.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for audio generation operations.
 * Used for generating audio for specific languages or with specific parameters.
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 */
data class AudioGenerationRequest(
    /**
     * Optional list of specific languages to generate audio for.
     * If null or empty, generates for all configured languages.
     * Languages must be valid BCP-47 codes (ta, en, hi, te, kn, ml).
     */
    @field:Size(max = 10, message = "Maximum 10 languages allowed")
    val languages: List<@Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$",
        message = "Language must be valid BCP-47 code (e.g., ta, en, hi)"
    ) String>? = null,
    
    /**
     * Optional voice profile to use for generation.
     * Defaults to "default" if not specified.
     */
    @field:Size(max = 100, message = "Voice profile must be at most 100 characters")
    val voiceProfile: String? = null,
    
    /**
     * Optional flag to force regeneration even if audio already exists.
     * Defaults to false.
     */
    val forceRegenerate: Boolean = false
)
