package com.tamixa.infrastructure.translation

import com.tamixa.application.port.TranslationClientPort
import com.tamixa.application.port.TranslatedContent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Simulated translation client (prefixes [lang] only).
 * Use app.translation.provider=openai for real translation.
 */
@Component
@Profile("!test")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["app.translation.provider"],
    havingValue = "simulated",
    matchIfMissing = true
)
class SimulatedTranslationClient(
    @Value("\${app.translation-pipeline.simulated-delay-ms:200}") private val simulatedDelayMs: Long
) : TranslationClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun translate(sourceLang: String, targetLang: String, text: String, timeoutMs: Long): String {
        if (simulatedDelayMs > 0) Thread.sleep(simulatedDelayMs)
        log.debug("Simulated translate {}->{} length={}", sourceLang, targetLang, text.length)
        return "[${targetLang}] $text"
    }

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
    ): TranslatedContent {
        if (simulatedDelayMs > 0) Thread.sleep(simulatedDelayMs)
        return TranslatedContent(
            title = title?.let { "[$targetLang] $it" },
            content = "[$targetLang] $content",
            moral = moral?.let { "[$targetLang] $it" },
            // Simulated client doesn't model paraphrase; snapshot is unavailable.
            titleBeforeParaphrase = null,
            contentBeforeParaphrase = null,
            moralBeforeParaphrase = null,
            parentContentNote = parentContentNote?.let { "[$targetLang] $it" },
            parentDiscussionPrompts = parentDiscussionPrompts?.map { "[$targetLang] $it" },
            speakAlongPrompt = speakAlongPrompt?.let { "[$targetLang] $it" },
        )
    }
}
