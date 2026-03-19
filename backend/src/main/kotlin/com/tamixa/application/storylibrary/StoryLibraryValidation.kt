package com.tamixa.application.storylibrary

/**
 * Validation rules for library story content.
 * - Minimum word count
 * - Script validation per language (Tamil, Hindi, Telugu, Kannada, Malayalam)
 * - Duplicate title detection (via repository)
 */
object StoryLibraryValidation {

    /** Minimum words for a valid story */
    const val MIN_WORD_COUNT = 50

    /** Tamil U+0B80–U+0BFF */
    private val TAMIL_PATTERN = Regex("""[\u0B80-\u0BFF]+""")
    /** Devanagari (Hindi) U+0900–U+097F */
    private val DEVANAGARI_PATTERN = Regex("""[\u0900-\u097F]+""")
    /** Telugu U+0C00–U+0C7F */
    private val TELUGU_PATTERN = Regex("""[\u0C00-\u0C7F]+""")
    /** Kannada U+0C80–U+0CFF */
    private val KANNADA_PATTERN = Regex("""[\u0C80-\u0CFF]+""")
    /** Malayalam U+0D00–U+0D7F */
    private val MALAYALAM_PATTERN = Regex("""[\u0D00-\u0D7F]+""")

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
            Result.failure(IllegalArgumentException("Story content must contain Tamil script (தமிழ் characters)"))
        }
    }

    /**
     * Validates that content contains the expected script for the given language.
     * Used for bulk generator and create to ensure stories are in the correct language.
     * English (en) has no script validation.
     */
    fun validateScriptForLanguage(content: String, language: String): Result<Unit> {
        val normalized = language.trim().lowercase()
        val (pattern, languageName) = when (normalized) {
            "ta", "tamil" -> TAMIL_PATTERN to "Tamil (தமிழ்)"
            "hi" -> DEVANAGARI_PATTERN to "Hindi (Devanagari)"
            "te" -> TELUGU_PATTERN to "Telugu"
            "kn" -> KANNADA_PATTERN to "Kannada"
            "ml" -> MALAYALAM_PATTERN to "Malayalam"
            else -> return Result.success(Unit) // en or unknown: no script validation
        }
        return if (pattern.containsMatchIn(content)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Story content must contain $languageName script. Bulk-generated content should be in the selected language."))
        }
    }
}
