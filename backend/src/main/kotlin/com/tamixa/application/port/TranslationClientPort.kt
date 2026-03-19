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
        timeoutMs: Long
    ): TranslatedContent
}

data class TranslatedContent(
    val title: String?,
    val content: String,
    val moral: String?
)
