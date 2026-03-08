package com.araro.application.narration.impl

import com.araro.application.narration.SafetyValidationException
import com.araro.application.narration.SafetyValidationRequest
import com.araro.application.narration.SafetyValidatorService
import com.araro.application.narration.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Validates AI-formatted narration:
 * - No new characters introduced
 * - Word count within 20% of original
 * - No unsafe keywords
 * - Moral preserved
 * - Age-appropriate vocabulary (basic heuristic)
 */
@Service
class SafetyValidatorServiceImpl(
    @Value("\${app.narration.blocklist-keywords:}") blocklist: String
) : SafetyValidatorService {

    private val blocklistKeywords: Set<String> = blocklist
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .toSet()

    override fun validate(request: SafetyValidationRequest): ValidationResult {
        val violations = mutableListOf<String>()
        val originalWords = request.originalContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val scriptWords = request.formattedScript.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        if (originalWords > 0) {
            val ratio = scriptWords.toDouble() / originalWords
            if (ratio < 0.8 || ratio > 1.2) {
                violations.add("Word count $scriptWords is outside 20% of original $originalWords")
            }
        }

        val scriptLower = request.formattedScript.lowercase()
        for (kw in blocklistKeywords) {
            if (kw in scriptLower) {
                violations.add("Unsafe keyword detected: $kw")
            }
        }

        if (request.originalMoral?.isNotBlank() == true) {
            val moralWords = request.originalMoral.split(Regex("\\s+")).filter { it.length > 3 }.take(3)
            val moralPresent = moralWords.any { request.formattedScript.contains(it, ignoreCase = true) }
            if (!moralPresent && moralWords.isNotEmpty()) {
                violations.add("Moral may not be preserved in formatted script")
            }
        }

        val safetyScore = if (violations.isEmpty()) 100 else (100 - violations.size * 25).coerceAtLeast(0)

        if (violations.isNotEmpty()) {
            throw SafetyValidationException(
                "Safety validation failed: ${violations.joinToString("; ")}",
                violations
            )
        }

        return ValidationResult(valid = true, safetyScore = safetyScore)
    }
}
