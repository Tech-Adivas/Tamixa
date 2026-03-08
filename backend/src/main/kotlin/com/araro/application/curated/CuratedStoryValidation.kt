package com.araro.application.curated

/**
 * Validation rules for curated story content.
 * - Minimum word count
 * - Tamil script validation (content must contain Tamil characters)
 * - Duplicate title detection (via repository)
 */
object CuratedStoryValidation {

    /** Minimum words for a valid story */
    const val MIN_WORD_COUNT = 50

    /** Tamil Unicode block: \p{IsTamil} or U+0B80–U+0BFF */
    private val TAMIL_PATTERN = Regex("""[\u0B80-\u0BFF]+""")

    /**
     * Validates that content has minimum word count.
     */
    fun validateMinWordCount(content: String): Result<Unit> {
        val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size >= MIN_WORD_COUNT) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Story must have at least $MIN_WORD_COUNT words. Current: ${words.size}"))
        }
    }

    /**
     * Validates that content contains Tamil script (for Tamil language stories).
     */
    fun validateTamilScript(content: String): Result<Unit> {
        val hasTamil = TAMIL_PATTERN.containsMatchIn(content)
        return if (hasTamil) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Story content must contain Tamil script (Tamil characters)"))
        }
    }
}
