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
        val translated: Boolean,
        val titleBeforeParaphrase: String? = null,
        val contentBeforeParaphrase: String? = null,
        val moralBeforeParaphrase: String? = null,
        val parentContentNote: String? = null,
        val parentDiscussionPrompts: List<String>? = null,
        val speakAlongPrompt: String? = null,
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
        moral: String?,
        forceParaphraseForSameLanguage: Boolean = false,
        bypassCache: Boolean = false,
        parentContentNote: String? = null,
        parentDiscussionPrompts: List<String>? = null,
        speakAlongPrompt: String? = null,
    ): TranslationResult {
        val src = sourceLang.trim().lowercase()
        val tgt = targetLang.trim().lowercase()
        if (src == tgt && !forceParaphraseForSameLanguage) {
            log.debug("Translation skipped: source==target ({})", tgt)
            return TranslationResult(
                title = title,
                content = content,
                moral = moral,
                translated = false,
                parentContentNote = parentContentNote?.trim()?.takeIf { it.isNotBlank() },
                parentDiscussionPrompts = parentDiscussionPrompts
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.takeIf { it.isNotEmpty() },
                speakAlongPrompt = speakAlongPrompt?.trim()?.takeIf { it.isNotBlank() },
            )
        }
        val sourceHash = contentHash(title, content, moral, parentContentNote, parentDiscussionPrompts, speakAlongPrompt)
        if (!bypassCache) {
            translationCache?.getByContentHash(sourceHash, tgt)?.let { cached ->
                log.info("Translation cache HIT {}->{} (hash={})", src, tgt, sourceHash.take(8))
                return TranslationResult(
                    title = cached.title,
                    content = cached.content,
                    moral = cached.moral,
                    translated = true,
                    parentContentNote = cached.parentContentNote,
                    parentDiscussionPrompts = cached.parentDiscussionPrompts,
                    speakAlongPrompt = cached.speakAlongPrompt,
                )
            }
        }
        // Non-Tamil languages get 1.5x timeout (translate+rewrite can be slower for hi,te,kn,ml)
        val timeoutMs = (baseTimeoutMs * 1.5).toLong()
        val translated = translationClient.translateStructured(
            sourceLang = src,
            targetLang = tgt,
            title = title,
            content = content,
            moral = moral,
            timeoutMs = timeoutMs,
            parentContentNote = parentContentNote,
            parentDiscussionPrompts = parentDiscussionPrompts,
            speakAlongPrompt = speakAlongPrompt,
        )
        val beforeTitle = translated.titleBeforeParaphrase?.let { trimLeadingSpecialSymbols(it) }
        val beforeContent = translated.contentBeforeParaphrase?.let { trimLeadingSpecialSymbols(it) }
        val beforeMoral = translated.moralBeforeParaphrase?.let { trimLeadingSpecialSymbols(it) }
        val result = TranslationResult(
            title = translated.title?.let { trimLeadingSpecialSymbols(it) },
            content = trimLeadingSpecialSymbols(translated.content),
            moral = translated.moral?.let { trimLeadingSpecialSymbols(it) },
            translated = true,
            titleBeforeParaphrase = beforeTitle,
            contentBeforeParaphrase = beforeContent,
            moralBeforeParaphrase = beforeMoral,
            parentContentNote = translated.parentContentNote?.let { trimLeadingSpecialSymbols(it) },
            parentDiscussionPrompts = translated.parentDiscussionPrompts
                ?.mapNotNull { trimLeadingSpecialSymbols(it).takeIf { s -> s.isNotBlank() } }
                ?.takeIf { it.isNotEmpty() },
            speakAlongPrompt = translated.speakAlongPrompt?.let { trimLeadingSpecialSymbols(it) },
        )
        if (!bypassCache) {
            translationCache?.setByContentHash(
                sourceHash,
                tgt,
                CachedTranslation(
                    title = result.title,
                    content = result.content,
                    moral = result.moral,
                    wordCount = result.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                    readingTimeMinutes = 0.0,
                    parentContentNote = result.parentContentNote,
                    parentDiscussionPrompts = result.parentDiscussionPrompts,
                    speakAlongPrompt = result.speakAlongPrompt,
                )
            )
        }
        return result
    }

    private fun contentHash(
        title: String?,
        content: String,
        moral: String?,
        parentContentNote: String?,
        parentDiscussionPrompts: List<String>?,
        speakAlongPrompt: String?,
    ): String {
        val p = parentDiscussionPrompts?.joinToString("\u001e") ?: ""
        val input = "${title ?: ""}|$content|${moral ?: ""}|${parentContentNote ?: ""}|$p|${speakAlongPrompt ?: ""}"
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
        t = stripLeadingNonLetterUnlessTtsMarker(t)
        return t.split("\n").joinToString("\n") { para ->
            stripLeadingNonLetterUnlessTtsMarker(para)
        }.trim()
    }

    /**
     * Strips leading punctuation/brackets unless the line is an inline TTS marker like `[Pause 500ms]`
     * (previously `^[^\\p{L}\\p{N}]+` ate `[` and broke markers).
     */
    private fun stripLeadingNonLetterUnlessTtsMarker(line: String): String {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("[") && trimmed.contains("]")) return trimmed
        return line.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trimStart()
    }
}
