package com.araro.domain.narration

/**
 * Structured output from emotion tagging.
 * Deterministic-first: no hallucinated content; segments preserve original text.
 *
 * Emotional realism is achieved via SSML prosody mapping, not script alteration.
 */
data class EmotionTaggedScript(
    /** Original script text (unchanged). */
    val originalScript: String,
    /** Segments with detected emotion. Order preserved. */
    val segments: List<EmotionSegment>,
    /** Total word count; used for validation. */
    val wordCount: Int
) {
    fun totalWordCount(): Int = segments.sumOf { it.wordCount() }

    /** Validation: total word count must not deviate significantly. */
    fun validateWordCountTolerance(tolerancePercent: Int = 15): Boolean {
        val expected = wordCount
        val actual = totalWordCount()
        if (expected == 0) return actual == 0
        val diffPercent = 100.0 * kotlin.math.abs(actual - expected) / expected
        return diffPercent <= tolerancePercent
    }
}

/**
 * A segment of script with a single dominant emotion.
 * No content added; only emotion label for SSML prosody.
 */
data class EmotionSegment(
    val text: String,
    val emotion: EmotionTag
) {
    fun wordCount(): Int = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
}

/**
 * Supported emotion tags for SSML prosody mapping.
 * Deterministic rules; no AI-generated emotions to prevent hallucination.
 */
enum class EmotionTag {
    /** Default soothing pace for bedtime. */
    CALM,
    /** Slight tension, slower build. */
    SOFT_SUSPENSE,
    /** Higher energy, faster. */
    EXCITED,
    /** Quieter, softer delivery. */
    WHISPER,
    /** Slight pitch increase for quoted speech (same voice). */
    DIALOGUE,
    /** Warm, inviting—questions and direct address (conversational feel). */
    CONVERSATIONAL
}
