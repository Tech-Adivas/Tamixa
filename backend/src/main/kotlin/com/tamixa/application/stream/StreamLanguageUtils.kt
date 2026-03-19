package com.tamixa.application.stream

/**
 * Normalizes language query param to DB/code form so "tamil" -> "ta", "hindi" -> "hi".
 * Ensures stream and narration lookups find the correct translation when client sends full name.
 */
object StreamLanguageUtils {
    private val ALIASES = mapOf(
        "tamil" to "ta", "hindi" to "hi", "english" to "en", "telugu" to "te",
        "kannada" to "kn", "malayalam" to "ml", "bengali" to "bn"
    )

    fun normalize(language: String): String {
        val raw = language.trim().lowercase().take(10)
        if (raw.isEmpty()) return "ta"
        return ALIASES[raw] ?: raw
    }
}
