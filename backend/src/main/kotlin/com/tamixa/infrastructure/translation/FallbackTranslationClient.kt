package com.tamixa.infrastructure.translation

import com.tamixa.application.port.TranslatedContent
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.infrastructure.llm.ProviderFallbackRunner

/**
 * Translation with a backup provider. Blank translated text counts as a failure.
 * Used by [OpenAiWithGeminiFallbackTranslationClient] and [GeminiWithOpenAiFallbackTranslationClient].
 */
class FallbackTranslationClient(
    val primary: TranslationClientPort,
    val fallback: TranslationClientPort,
    private val runner: ProviderFallbackRunner,
) : TranslationClientPort {

    override fun translate(sourceLang: String, targetLang: String, text: String, timeoutMs: Long): String =
        runner.run(
            "translate $sourceLang->$targetLang",
            isUsable = { it.isNotBlank() || text.isBlank() },
            primary = { primary.translate(sourceLang, targetLang, text, timeoutMs) },
            fallback = { fallback.translate(sourceLang, targetLang, text, timeoutMs) },
        )

    override fun translateStructured(
        sourceLang: String,
        targetLang: String,
        title: String?,
        content: String,
        moral: String?,
        timeoutMs: Long,
        parentContentNote: String?,
        parentDiscussionPrompts: List<String>?,
        speakAlongPrompt: String?,
    ): TranslatedContent =
        runner.run(
            "translateStructured $sourceLang->$targetLang",
            isUsable = { it.content.isNotBlank() || content.isBlank() },
            primary = {
                primary.translateStructured(
                    sourceLang, targetLang, title, content, moral, timeoutMs,
                    parentContentNote, parentDiscussionPrompts, speakAlongPrompt,
                )
            },
            fallback = {
                fallback.translateStructured(
                    sourceLang, targetLang, title, content, moral, timeoutMs,
                    parentContentNote, parentDiscussionPrompts, speakAlongPrompt,
                )
            },
        )
}
