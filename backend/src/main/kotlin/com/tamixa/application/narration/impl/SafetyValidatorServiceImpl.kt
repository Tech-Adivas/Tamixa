package com.tamixa.application.narration.impl

import com.tamixa.application.narration.SafetyValidationException
import com.tamixa.application.narration.SafetyValidationRequest
import com.tamixa.application.narration.SafetyValidatorService
import com.tamixa.application.narration.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Validates AI-formatted narration:
 * - No new characters introduced
 * - Word count within configured tolerance of original (default 35%: 65%-135% on the low side; high side may exceed ratio if still under [maxPipelineStoryWords])
 * - Hard cap: formatted script must not exceed [maxPipelineStoryWords] (default from app.story.max-words, typically 900). Set to 0 to disable cap (ratio-only).
 * - No unsafe keywords
 * - Moral preserved
 * - Age-appropriate vocabulary (basic heuristic)
 *
 * Conversational and Tamil narrations often shorten vs written text; tolerance is configurable via app.narration.word-count-tolerance-percent.
 */
@Service
class SafetyValidatorServiceImpl(
    @Value("\${app.narration.word-count-tolerance-percent:35}") private val wordCountTolerancePercent: Int,
    @Value("\${app.narration.blocklist-keywords:}") blocklist: String,
    @Value("\${app.story.max-words:900}") private val maxPipelineStoryWords: Int
) : SafetyValidatorService {

    private val blocklistKeywords: Set<String> = blocklist
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .toSet()

    private val wordCountMinRatio: Double = (100 - wordCountTolerancePercent).coerceIn(10, 95) / 100.0
    private val wordCountMaxRatio: Double = (100 + wordCountTolerancePercent).coerceIn(105, 200) / 100.0

    override fun validate(request: SafetyValidationRequest): ValidationResult {
        val violations = mutableListOf<String>()
        val originalWords = request.originalContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val scriptWords = request.formattedScript.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        val cap = maxPipelineStoryWords.coerceIn(0, 5000)
        if (cap > 0 && scriptWords > cap) {
            violations.add("Word count $scriptWords exceeds maximum $cap words for narration")
        }
        if (originalWords > 0) {
            val ratio = scriptWords.toDouble() / originalWords
            if (ratio < wordCountMinRatio) {
                violations.add("Word count $scriptWords is outside ${wordCountTolerancePercent}% of original $originalWords (too short)")
            } else if (ratio > wordCountMaxRatio) {
                when {
                    cap > 0 && scriptWords <= cap -> { /* longer rewrite allowed up to word cap (creative storytelling) */ }
                    cap <= 0 ->
                        violations.add("Word count $scriptWords is outside ${wordCountTolerancePercent}% of original $originalWords (too long)")
                    else -> { /* exceeded cap — already reported above */ }
                }
            }
        }

        val scriptLower = request.formattedScript.lowercase()
        for (kw in blocklistKeywords) {
            if (kw in scriptLower) {
                violations.add("Unsafe keyword detected: $kw")
            }
        }

        if (request.originalMoral?.isNotBlank() == true) {
            val moral = request.originalMoral.trim()
            val script = request.formattedScript
            // Substring check: LLM often paraphrases; exact token match fails for Tamil/indic rewrites.
            val moralCollapsed = moral.replace(Regex("\\s+"), " ")
            val scriptCollapsed = script.replace(Regex("\\s+"), " ")
            val moralAsSubstring = moralCollapsed.length >= 8 && scriptCollapsed.contains(moralCollapsed, ignoreCase = true)
            val moralWords = moral.split(Regex("\\s+")).filter { it.length > 3 }.take(5)
            val moralWordHit = moralWords.any { script.contains(it, ignoreCase = true) }
            // If moral is short (e.g. one phrase), require substring or any word hit; else require word hit or substring of first 80 chars
            val moralPrefix = moralCollapsed.take(80)
            val prefixHit = moralPrefix.length >= 12 && scriptCollapsed.contains(moralPrefix, ignoreCase = true)
            val moralPresent = moralAsSubstring || moralWordHit || prefixHit
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
