package com.tamixa.common

/**
 * Which writing system a text uses, for the app's languages (en, ta, hi, te, kn, ml).
 * Used to catch titles stored in the wrong language (e.g. a Telugu title on a Tamil story).
 */
object ScriptLanguage {

    private val INDIC_SCRIPTS: Map<String, Regex> = linkedMapOf(
        "ta" to Regex("[஀-௿]"),
        "hi" to Regex("[ऀ-ॿ]"),
        "te" to Regex("[ఀ-౿]"),
        "kn" to Regex("[ಀ-೿]"),
        "ml" to Regex("[ഀ-ൿ]"),
    )

    private val LANGUAGE_NAMES = mapOf(
        "en" to "English", "ta" to "Tamil", "hi" to "Hindi", "te" to "Telugu", "kn" to "Kannada", "ml" to "Malayalam",
    )

    /** Language codes of the Indic scripts present in [text]. */
    fun indicScriptsIn(text: String): Set<String> =
        INDIC_SCRIPTS.filterValues { it.containsMatchIn(text) }.keys

    /**
     * True when [text] is written for [language]: an Indic language needs its own script and no other Indic script
     * (Latin words such as "UPI" are fine); English needs no Indic script. Blank text and other languages pass.
     */
    fun matches(text: String?, language: String): Boolean {
        if (text.isNullOrBlank()) return true
        val lang = language.trim().lowercase().take(10)
        val found = indicScriptsIn(text)
        return when {
            lang == "en" -> found.isEmpty()
            lang in INDIC_SCRIPTS -> lang in found && (found - lang).isEmpty()
            else -> true
        }
    }

    /** Language whose script [text] is written in: a single Indic script, else "en" when it has none, else null (mixed). */
    fun detect(text: String): String? {
        val found = indicScriptsIn(text)
        return when (found.size) {
            0 -> "en"
            1 -> found.first()
            else -> null
        }
    }

    /** Human name for messages, e.g. "Telugu"; "mixed scripts" when [detect] can't tell. */
    fun nameOf(language: String?): String = LANGUAGE_NAMES[language?.lowercase()] ?: (language ?: "mixed scripts")

    /** Message for a title in the wrong language, e.g. "Title is in Telugu but the story is Tamil". */
    fun titleMismatchMessage(title: String, language: String): String =
        "Title is in ${nameOf(detect(title))} but the story is ${nameOf(language)}. Fix the title (or regenerate it) before saving."
}
