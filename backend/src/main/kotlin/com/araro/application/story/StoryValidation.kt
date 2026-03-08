package com.araro.application.story

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** Max title length for structured output validation (safety: limit abuse surface). */
private const val MAX_TITLE_LENGTH = 80

/** Words per minute for reading-time calculation; duration must match word count within tolerance. */
private const val WORDS_PER_MINUTE = 150

/** Tolerance: estimated duration may be ±40% of expected from word count. */
private const val DURATION_TOLERANCE = 0.4

/**
 * Strict output validation for structured story payload.
 * - Title: max 80 chars
 * - Story text: age-based max word count
 * - No political / religious / violent references (via safety rules)
 * - Moral must be present
 * - Estimated duration must match word count (within tolerance)
 *
 * On validation failure: caller retries once with fallback prompt; else marks FAILED.
 */
@Component
class StoryValidation(
    @Value("\${app.story.max-words:750}") private val maxWords: Int,
    private val safetyMiddleware: StorySafetyMiddleware,
    private val safetyValidationRules: StorySafetyValidationRules,
    private val storyPromptBuilder: StoryPromptBuilder
) {

    /**
     * Validates payload for given [age]. Throws if invalid.
     * Age-based max words override global max when stricter.
     */
    fun validate(payload: StructuredStoryPayload, age: Int) {
        // Title: max 80 chars
        if (payload.title.isBlank()) throw InvalidStoryRequestException("Story title is missing")
        if (payload.title.length > MAX_TITLE_LENGTH) {
            throw InvalidStoryRequestException("Story title exceeds maximum length (${payload.title.length} chars, max $MAX_TITLE_LENGTH)")
        }

        // Moral must be present
        if (payload.moral.isBlank()) {
            throw InvalidStoryRequestException("Story moral is missing")
        }

        // Duration range
        if (payload.estimatedDurationSeconds !in 1..3600) {
            throw InvalidStoryRequestException("estimated_duration_seconds must be between 1 and 3600")
        }

        val words = payload.storyText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        // Age-based max word count (stricter for younger children)
        val ageMaxWords = storyPromptBuilder.maxWordsForAge(age).coerceAtMost(maxWords)
        if (wordCount > ageMaxWords) {
            throw StoryTooLongException(
                "Story exceeds maximum length ($wordCount words, max $ageMaxWords for age $age)"
            )
        }
        if (wordCount < 10) {
            throw InvalidStoryRequestException("Story too short (minimum 10 words)")
        }

        // Estimated duration must match word count: expected seconds = (words/150)*60
        val expectedSeconds = (wordCount.toDouble() / WORDS_PER_MINUTE * 60).toInt().coerceAtLeast(1)
        val actual = payload.estimatedDurationSeconds
        val minAllowed = (expectedSeconds * (1 - DURATION_TOLERANCE)).toInt().coerceAtLeast(1)
        val maxAllowed = (expectedSeconds * (1 + DURATION_TOLERANCE)).toInt()
        if (actual !in minAllowed..maxAllowed) {
            throw InvalidStoryRequestException(
                "estimated_duration_seconds ($actual) does not match word count ($wordCount words, expected ~$expectedSeconds sec)"
            )
        }

        // Content safety: blocklist, political/religious/violent (via safety rules)
        if (!safetyMiddleware.isGeneratedContentChildSafe(payload.storyText)) {
            throw ContentModerationException("Generated story contains disallowed vocabulary")
        }
        safetyValidationRules.validate(payload.storyText, age)
    }
}
