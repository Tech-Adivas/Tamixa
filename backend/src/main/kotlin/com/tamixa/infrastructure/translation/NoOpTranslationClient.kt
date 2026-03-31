package com.tamixa.infrastructure.translation

import com.tamixa.application.port.TranslationClientPort
import com.tamixa.application.port.TranslatedContent
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpTranslationClient : TranslationClientPort {

    override fun translate(sourceLang: String, targetLang: String, text: String, timeoutMs: Long): String = text

    override fun translateStructured(
        sourceLang: String,
        targetLang: String,
        title: String?,
        content: String,
        moral: String?,
        timeoutMs: Long
    ): TranslatedContent = TranslatedContent(
        title = title,
        content = content,
        moral = moral,
        titleBeforeParaphrase = null,
        contentBeforeParaphrase = null,
        moralBeforeParaphrase = null
    )
}
