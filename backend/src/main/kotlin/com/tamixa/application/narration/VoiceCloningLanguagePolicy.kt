package com.tamixa.application.narration

/**
 * Canonical policy for voice-cloning language support.
 *
 * - **Tamil (ta)**: XTTS does not support it; cloned-voice Tamil must use ElevenLabs only.
 *   Never send Tamil to XTTS (would 500 or wrong language).
 * - **Other Indic (hi, te, etc.)**: XTTS supports hi; others may be mapped for synthesis.
 *
 * Use [isTamil] to guard XTTS fallback and [normalizeForCloning] for consistent code (ta/tamil -> ta).
 */
object VoiceCloningLanguagePolicy {

    /** Language codes that XTTS does not support; require ElevenLabs for cloned voice. */
    private val XTTS_UNSUPPORTED_CLONED = setOf("ta")

    private fun normalizedLang(language: String): String =
        language.trim().lowercase().take(5).substringBefore('-')

    /** True if the language is Tamil (ta or tamil). */
    fun isTamil(language: String): Boolean {
        val n = normalizedLang(language)
        return n == "ta" || n == "tamil"
    }

    /** True if this language must not be sent to XTTS for cloned voice (use ElevenLabs only). */
    fun requiresElevenLabsForCloned(language: String): Boolean =
        isTamil(language)
}
