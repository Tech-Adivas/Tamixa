package com.tamixa.application.storylibrary

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Validation rules for library story content.
 * - Minimum word count
 * - Script validation per language (Tamil, Hindi, Telugu, Kannada, Malayalam)
 * - Duplicate title detection (via repository)
 */
object StoryLibraryValidation {

    private val jsonMapper = ObjectMapper()

    /** Minimum words for a valid story */
    const val MIN_WORD_COUNT = 50

    const val MAX_INTERACTIVE_GRAPH_CHARS = 100_000

    /** Matches mobile/web [isSimulatorStory] — theme or category must use Learn · Simulator for branching graphs. */
    private val simulatorMetadataRegex = Regex("^learn\\s*·\\s*simulator\\b", RegexOption.IGNORE_CASE)

    /**
     * True when JSON has a non-empty `segments` object (branching episode payload).
     * Empty `{}` or missing segments does not count.
     */
    fun interactiveGraphHasBranchingPayload(graphJson: String?): Boolean {
        if (graphJson.isNullOrBlank()) return false
        val node = runCatching { jsonMapper.readTree(graphJson) }.getOrNull() ?: return false
        if (!node.isObject) return false
        val segments = node.get("segments") ?: return false
        return segments.isObject && segments.size() > 0
    }

    fun themeOrCategoryLooksLikeSimulator(theme: String, category: String?): Boolean {
        val t = theme.trim()
        val c = category?.trim().orEmpty()
        return simulatorMetadataRegex.containsMatchIn(t) || simulatorMetadataRegex.containsMatchIn(c)
    }

    /**
     * @throws IllegalArgumentException when a branching graph is stored without Learn · Simulator metadata.
     */
    fun validateInteractiveGraphThemeAlignment(theme: String, category: String?, graphJson: String?) {
        if (!interactiveGraphHasBranchingPayload(graphJson)) return
        if (themeOrCategoryLooksLikeSimulator(theme, category)) return
        throw IllegalArgumentException(
            "Interactive graph with segments requires theme or category to start with \"Learn · Simulator\" " +
                "(e.g. Learn · Simulator · Digital Safety). See docs/admin/EDU_METADATA_CONVENTIONS.md.",
        )
    }

    /**
     * Normalizes optional interactive graph JSON from admin/API.
     * @throws IllegalArgumentException if non-blank but not valid JSON or too large.
     */
    fun normalizeInteractiveGraphJson(raw: String?): String? {
        if (raw == null) return null
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (t.length > MAX_INTERACTIVE_GRAPH_CHARS) {
            throw IllegalArgumentException("Interactive graph JSON exceeds maximum size ($MAX_INTERACTIVE_GRAPH_CHARS characters).")
        }
        runCatching { jsonMapper.readTree(t) }.getOrElse {
            throw IllegalArgumentException("Interactive graph must be valid JSON.")
        }
        return t
    }

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
