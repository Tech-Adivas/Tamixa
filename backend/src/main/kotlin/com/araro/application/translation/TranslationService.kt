package com.araro.application.translation

import com.araro.application.port.TranslationClientPort
import com.araro.application.port.TranslatedContent
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Translation service with conditional logic: skips LLM call when source == target.
 * Deterministic two-step pipeline: (1) translate only if needed, (2) rewrite in separate step.
 * Sanitizes translated output to strip leading special symbols that TTS reads aloud awkwardly.
 */
@Service
class TranslationService(
    private val translationClient: TranslationClientPort,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sourceLanguage get() = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
    private val baseTimeoutMs get() = appProperties.translationPipeline.translationTimeoutMs

    data class TranslationResult(
        val title: String?,
        val content: String,
        val moral: String?,
        val translated: Boolean
    )

    /**
     * Translate structured content from source to target language.
     * Idempotent-friendly: when source == target, returns original content without external call.
     */
    fun translateIfNeeded(
        sourceLang: String,
        targetLang: String,
        title: String?,
        content: String,
        moral: String?
    ): TranslationResult {
        val src = sourceLang.trim().lowercase()
        val tgt = targetLang.trim().lowercase()
        if (src == tgt) {
            log.debug("Translation skipped: source==target ({})", tgt)
            return TranslationResult(title = title, content = content, moral = moral, translated = false)
        }
        // Non-Tamil languages get 1.5x timeout (translate+rewrite can be slower for hi,te,kn,ml)
        val timeoutMs = (baseTimeoutMs * 1.5).toLong()
        val translated = translationClient.translateStructured(
            sourceLang = src,
            targetLang = tgt,
            title = title,
            content = content,
            moral = moral,
            timeoutMs = timeoutMs
        )
        return TranslationResult(
            title = translated.title?.let { trimLeadingSpecialSymbols(it) },
            content = trimLeadingSpecialSymbols(translated.content),
            moral = translated.moral?.let { trimLeadingSpecialSymbols(it) },
            translated = true
        )
    }

    /**
     * Trims leading special symbols, labels (content:, title:, moral:), and stray quotes/commas
     * that TTS may read aloud. Keeps letters, numbers, and natural sentence-start chars.
     */
    private fun trimLeadingSpecialSymbols(text: String): String {
        if (text.isBlank()) return text
        var t = text.trim()
        t = Regex("^\\s*\"?(content|title|moral)\"?:?\\s*[\"']?", RegexOption.IGNORE_CASE).replace(t, "")
        t = Regex("^[\"',;:\\s]+").replace(t, "")
        t = t.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trimStart()
        return t.split("\n").joinToString("\n") { para ->
            para.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trimStart()
        }.trim()
    }
}
