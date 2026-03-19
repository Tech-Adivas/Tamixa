package com.tamixa.application.translation

import com.tamixa.application.port.CachedTranslation
import com.tamixa.application.port.TranslationCachePort
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.application.port.TranslatedContent
import com.tamixa.infrastructure.config.AppProperties
import org.springframework.beans.factory.annotation.Autowired
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest

/**
 * Translation service with conditional logic: skips LLM call when source == target.
 * Uses content-hash cache to avoid repeated OpenAI calls when retrying pipeline or running multiple times.
 * Sanitizes translated output to strip leading special symbols that TTS reads aloud awkwardly.
 */
@Service
class TranslationService(
    private val translationClient: TranslationClientPort,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val translationCache: TranslationCachePort?
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
     * Skips when source == target. Uses cache when same content was translated before (avoids OpenAI on retries).
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
        val sourceHash = contentHash(title, content, moral)
        translationCache?.getByContentHash(sourceHash, tgt)?.let { cached ->
            log.info("Translation cache HIT {}->{} (hash={})", src, tgt, sourceHash.take(8))
            return TranslationResult(
                title = cached.title,
                content = cached.content,
                moral = cached.moral,
                translated = true
            )
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
        val result = TranslationResult(
            title = translated.title?.let { trimLeadingSpecialSymbols(it) },
            content = trimLeadingSpecialSymbols(translated.content),
            moral = translated.moral?.let { trimLeadingSpecialSymbols(it) },
            translated = true
        )
        translationCache?.setByContentHash(
            sourceHash,
            tgt,
            CachedTranslation(
                title = result.title,
                content = result.content,
                moral = result.moral,
                wordCount = result.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                readingTimeMinutes = 0.0
            )
        )
        return result
    }

    private fun contentHash(title: String?, content: String, moral: String?): String {
        val input = "${title ?: ""}|$content|${moral ?: ""}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
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
