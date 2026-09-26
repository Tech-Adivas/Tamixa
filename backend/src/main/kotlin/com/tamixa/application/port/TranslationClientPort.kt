package com.tamixa.application.port

/**
 * Translates text from source language (Tamil) to target language.
 * Implementations: OpenAI, Google Translate API, or simulated for testing.
 */
interface TranslationClientPort {

    fun translate(
        sourceLang: String,
        targetLang: String,
        text: String,
        timeoutMs: Long
    ): String

    fun translateStructured(
        sourceLang: String,
        targetLang: String,
        title: String?,
        content: String,
        moral: String?,
        timeoutMs: Long,
        parentContentNote: String? = null,
        parentDiscussionPrompts: List<String>? = null,
        speakAlongPrompt: String? = null,
    ): TranslatedContent
}

data class TranslatedContent(
    val title: String?,
    val content: String,
    val moral: String?,
    /**
     * Optional snapshot captured BEFORE the mandatory "paraphrase pass" that
     * happens after translation. Used for UI diff/highlighting.
     *
     * When null, the caller cannot show a before/after paraphrase diff.
     */
    val titleBeforeParaphrase: String? = null,
    val contentBeforeParaphrase: String? = null,
    val moralBeforeParaphrase: String? = null,
    /** Translated parent-facing note; null when source had none or model omitted. */
    val parentContentNote: String? = null,
    val parentDiscussionPrompts: List<String>? = null,
    val speakAlongPrompt: String? = null,
)
