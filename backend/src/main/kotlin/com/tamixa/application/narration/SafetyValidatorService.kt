package com.tamixa.application.narration

/**
 * Validates AI-formatted narration for child safety and content integrity.
 * Throws [SafetyValidationException] if validation fails.
 */
interface SafetyValidatorService {

    /**
     * Validate formatted script against original story.
     * Checks: no new characters, word count within bounds, no unsafe keywords,
     * moral preserved, age-appropriate vocabulary.
     */
    fun validate(request: SafetyValidationRequest): ValidationResult
}

data class SafetyValidationRequest(
    val originalContent: String,
    val originalMoral: String?,
    val formattedScript: String,
    val age: Int,
    val language: String
)

data class ValidationResult(
    val valid: Boolean,
    val safetyScore: Int,
    val violations: List<String> = emptyList()
)

class SafetyValidationException(message: String, val violations: List<String>) : RuntimeException(message)
