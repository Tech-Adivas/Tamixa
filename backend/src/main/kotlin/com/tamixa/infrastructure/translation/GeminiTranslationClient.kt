package com.tamixa.infrastructure.translation

import com.tamixa.application.port.TranslatedContent
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.gemini.GeminiApiClient
import com.tamixa.infrastructure.http.ApiErrorExtractor
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

/**
 * Real translation via Google Gemini (JSON mode), mirroring [OpenAITranslationClient] prompts.
 * Enable with `app.translation.provider=gemini` and `GEMINI_API_KEY` set.
 */
@Component
@ConditionalOnProperty(name = ["app.translation.provider"], havingValue = "gemini")
class GeminiTranslationClient(
    private val geminiApiClient: GeminiApiClient,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    private val appProperties: AppProperties,
    @Value("\${app.story.max-words:900}") private val maxStoryWords: Int,
) : TranslationClientPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val langNames = mapOf(
        "ta" to "Tamil",
        "en" to "English",
        "hi" to "Hindi",
        "kn" to "Kannada",
        "te" to "Telugu",
        "ml" to "Malayalam",
        "bn" to "Bengali",
    )

    override fun translate(sourceLang: String, targetLang: String, text: String, timeoutMs: Long): String {
        val result = translateStructured(sourceLang, targetLang, null, text, null, timeoutMs)
        return result.content
    }

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [IllegalStateException::class],
        maxAttempts = 2,
        backoff = Backoff(delay = 1000, multiplier = 1.5),
    )
    override fun translateStructured(
        sourceLang: String,
        targetLang: String,
        title: String?,
        content: String,
        moral: String?,
        timeoutMs: Long,
    ): TranslatedContent {
        log.info("Gemini translation start {}->{} contentLen={}", sourceLang, targetLang, content.length)
        if (appProperties.llm.gemini.apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key not configured. Set GEMINI_API_KEY for translation.")
        }
        val srcName = langNames[sourceLang] ?: sourceLang
        val tgtName = langNames[targetLang] ?: targetLang
        val wordCap = maxStoryWords.coerceIn(200, 2500)
        val systemPrompt = """
            You are a translator and storyteller for children's stories. Translate from $srcName to $tgtName.
            Output valid JSON only with keys:
              title (or null),
              content,
              moral (or null),
              title_before_paraphrase (or null),
              content_before_paraphrase,
              moral_before_paraphrase (or null).
            CRITICAL: title and moral MUST be translated to $tgtName. Never return them in $srcName when target is $tgtName.
            When TITLE and/or MORAL appear in the user message, you MUST return non-null JSON strings for "title" and "moral" in native $tgtName script (Telugu/Kannada/Malayalam: use Telugu/Kannada/Malayalam script—never leave title or moral in Tamil when translating from Tamil).
            Preserve the story's meaning, emotional arc, and cultural context. Use clear, age-appropriate vocabulary in $tgtName—everyday words a caregiver would use aloud—not overly formal or literal word-for-word calques when a natural native phrase exists.
            Continuity: Keep the same scene order and causal flow as the source. Do not skip paragraphs, merge unrelated beats into confusing jumps, or drop resolutions. If the source transition is weak, add a short bridging phrase in $tgtName so the narration never feels discontinuous. Do not invent new plot points, twists, or named characters that are not implied by the source.
            Translation stage: Produce a faithful translation output for title/content/moral.
            Paraphrase pass (mandatory): After translating, paraphrase the title/content/moral into a native, conversational $tgtName style (use different phrasing and sentence structures, avoid calques).
            IMPORTANT: Return the translation-stage (BEFORE paraphrase) output in the *_before_paraphrase keys, and the final paraphrased output in the non-suffixed keys. The story must still match the source meaning and beats exactly: same plot events in the same order, same emotional arc, and the moral intent preserved.
            Style: Natural conversational storytelling in $tgtName—as if a native caregiver is telling a child aloud—not stiff or literal. Prefer vivid, warm phrasing; it is good if the retelling feels lively and immersive.
            Length (critical): The "content" field must be at most $wordCap words (count in $tgtName). If the source is already at or under $wordCap words, translate the full text—do NOT summarize, shorten, or tighten for brevity; keep comparable length and richness. Only when the source clearly exceeds $wordCap words should you compress, preserving every major scene and the moral in order. When the source is shorter than $wordCap words, expand freely with natural dialogue, sensory detail, and oral storytelling color within the same scenes and facts—aim to use much of the available word budget up to $wordCap so the story feels full for narration, without changing what happens or who is in the story.
            CRITICAL: Preserve all inline narration markers exactly as-is in the content_before_paraphrase and content. These include [Pause 500ms], [Pause 1s], [Happy tone], [Soft voice], [Warm tone], [Calm], [Whisper], [Excited]. Do NOT remove, translate, or alter them—they are TTS instructions.
            Do NOT use bullets (•), asterisks (*), em-dashes (—), or other special symbols at the start of lines or paragraphs. Output plain text suitable for text-to-speech.
        """.trimIndent()
        val parts = buildList {
            title?.let { add("TITLE: $it") }
            add("CONTENT:\n$content")
            moral?.let { add("MORAL: $it") }
        }
        val userPrompt =
            "Translate the following story. Return JSON with keys exactly: \"title\", \"content\", \"moral\", \"title_before_paraphrase\", \"content_before_paraphrase\", \"moral_before_paraphrase\" (use null only if the source truly had no title/moral). All string values in $tgtName.\n\n${parts.joinToString("\n\n")}"
        val rawCompletion = completeJsonChat(systemPrompt, userPrompt, maxTokens = 12288, temperature = 0.5)
        val result = fillMissingTitleMoral(
            parseTranslationResponse(rawCompletion, title, content, moral, sourceLang, targetLang),
            originalTitle = title,
            originalMoral = moral,
            sourceLang = sourceLang,
            targetLang = targetLang,
        )
        log.info("Gemini translation success {}->{} resultLen={}", sourceLang, targetLang, result.content.length)
        return result
    }

    private fun completeJsonChat(systemPrompt: String, userPrompt: String, maxTokens: Int, temperature: Double): String {
        if (appProperties.llm.gemini.apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key not configured. Set GEMINI_API_KEY for translation.")
        }
        return try {
            val (text, _) = geminiApiClient.generateContent(
                systemInstruction = systemPrompt,
                userText = userPrompt,
                maxOutputTokens = maxTokens.coerceIn(64, 16384),
                temperature = temperature.coerceIn(0.0, 1.0),
                responseMimeType = "application/json",
            )
            text.trim()
        } catch (e: Exception) {
            val msg = ApiErrorExtractor.extract(e, objectMapper)
            throw IllegalStateException("Translation failed: $msg", e)
        }
    }

    private fun fillMissingTitleMoral(
        parsed: TranslatedContent,
        originalTitle: String?,
        originalMoral: String?,
        sourceLang: String,
        targetLang: String,
    ): TranslatedContent {
        val src = sourceLang.trim().lowercase()
        val tgt = targetLang.trim().lowercase()
        if (src == tgt) return parsed
        var t = parsed.title?.takeIf { it.isNotBlank() }
        var m = parsed.moral?.takeIf { it.isNotBlank() }
        if (t.isNullOrBlank() && !originalTitle.isNullOrBlank()) {
            val back = translatePlainPhrase(originalTitle, src, tgt)?.let { sanitizeTranslationText(it) }
            t = back?.takeIf { if (src == "ta") !containsTamilScript(it) else true }
            if (t != null) log.info("Translation title filled via phrase fallback {}->{}", src, tgt)
        }
        if (m.isNullOrBlank() && !originalMoral.isNullOrBlank()) {
            val back = translatePlainPhrase(originalMoral, src, tgt)?.let { sanitizeTranslationText(it) }
            m = back?.takeIf { if (src == "ta") !containsTamilScript(it) else true }
            if (m != null) log.info("Translation moral filled via phrase fallback {}->{}", src, tgt)
        }
        return TranslatedContent(
            title = t ?: parsed.title,
            content = parsed.content,
            moral = m ?: parsed.moral,
        )
    }

    private fun translatePlainPhrase(phrase: String, sourceLang: String, targetLang: String): String? {
        val src = sourceLang.trim().lowercase()
        val tgt = targetLang.trim().lowercase()
        if (phrase.isBlank() || src == tgt) return phrase.trim().takeIf { it.isNotBlank() }
        val srcName = langNames[src] ?: sourceLang
        val tgtName = langNames[tgt] ?: targetLang
        val system =
            "Translate from $srcName to $tgtName. Output ONLY the translated text—no quotes, no JSON, no labels, no explanation. One short line suitable for a children's story title or moral."
        return try {
            val (text, _) = geminiApiClient.generateContent(
                systemInstruction = system,
                userText = phrase.trim(),
                maxOutputTokens = 256,
                temperature = 0.15,
                responseMimeType = null,
            )
            text.lines().firstOrNull { it.isNotBlank() }?.trim()
        } catch (e: Exception) {
            log.warn("Plain phrase translation failed {}->{}: {}", src, tgt, e.message)
            null
        }
    }

    private fun jsonTextField(node: JsonNode, vararg keys: String): String? {
        if (!node.isObject) return null
        val wanted = keys.map { it.lowercase() }.toSet()
        val it = node.fields()
        while (it.hasNext()) {
            val e = it.next()
            if (e.key.lowercase() in wanted) {
                val v = e.value ?: continue
                if (v.isNull) return null
                return v.asText()?.trim()?.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun containsTamilScript(text: String?): Boolean =
        text != null && Regex("[\u0B80-\u0BFF]").containsMatchIn(text)

    private fun recordTamilLeakRecovery(field: String, targetLang: String, success: Boolean) {
        meterRegistry.counter(
            "translation_tamil_leak_recovery_total",
            "field", field.ifBlank { "unknown" }.lowercase(),
            "target", targetLang.ifBlank { "unknown" }.lowercase(),
            "outcome", if (success) "success" else "failure",
        ).increment()
    }

    private fun parseTranslationResponse(
        raw: String,
        originalTitle: String?,
        originalContent: String,
        originalMoral: String?,
        sourceLang: String,
        targetLang: String,
    ): TranslatedContent {
        return try {
            val json = normalizeJsonPayload(raw)
            val node = objectMapper.readTree(json)
            val rawContent = jsonTextField(node, "content", "story_text", "storyText", "body", "text")
                ?: throw IllegalStateException("Missing or empty content in translation response")
            val rawContentBefore = jsonTextField(
                node,
                "content_before_paraphrase",
                "contentBeforeParaphrase",
                "content_before",
                "translated_content",
            )
            val rawTitle = sanitizeTranslationText(jsonTextField(node, "title", "heading"))
            val rawMoral = sanitizeTranslationText(jsonTextField(node, "moral", "lesson"))
            val rawTitleBefore = sanitizeTranslationText(
                jsonTextField(node, "title_before_paraphrase", "titleBeforeParaphrase"),
            )
            val rawMoralBefore = sanitizeTranslationText(
                jsonTextField(node, "moral_before_paraphrase", "moralBeforeParaphrase"),
            )
            val src = sourceLang.trim().lowercase()
            val tgt = targetLang.trim().lowercase()
            val (finalTitle, finalMoral) = if (src == tgt) {
                (rawTitle ?: originalTitle) to (rawMoral ?: originalMoral)
            } else if (src == "ta") {
                val t = if (rawTitle != null && containsTamilScript(rawTitle)) {
                    val recovered = translatePlainPhrase(rawTitle, src, tgt)?.let { sanitizeTranslationText(it) }
                        ?.takeIf { !containsTamilScript(it) }
                    if (recovered == null) {
                        recordTamilLeakRecovery(field = "title", targetLang = targetLang, success = false)
                        log.warn(
                            "Translation title contains Tamil script for target={}; recovery failed, keeping null",
                            targetLang,
                        )
                    } else {
                        recordTamilLeakRecovery(field = "title", targetLang = targetLang, success = true)
                        log.info("Translation title recovery succeeded for target={}", targetLang)
                    }
                    recovered
                } else rawTitle
                val m = if (rawMoral != null && containsTamilScript(rawMoral)) {
                    val recovered = translatePlainPhrase(rawMoral, src, tgt)?.let { sanitizeTranslationText(it) }
                        ?.takeIf { !containsTamilScript(it) }
                    if (recovered == null) {
                        recordTamilLeakRecovery(field = "moral", targetLang = targetLang, success = false)
                        log.warn(
                            "Translation moral contains Tamil script for target={}; recovery failed, keeping null",
                            targetLang,
                        )
                    } else {
                        recordTamilLeakRecovery(field = "moral", targetLang = targetLang, success = true)
                        log.info("Translation moral recovery succeeded for target={}", targetLang)
                    }
                    recovered
                } else rawMoral
                t to m
            } else {
                rawTitle to rawMoral
            }

            val (beforeTitle, beforeMoral) = if (src == tgt) {
                (rawTitleBefore ?: finalTitle) to (rawMoralBefore ?: finalMoral)
            } else if (src == "ta") {
                val t = if (rawTitleBefore != null && containsTamilScript(rawTitleBefore)) {
                    val recovered = translatePlainPhrase(rawTitleBefore, src, tgt)?.let { sanitizeTranslationText(it) }
                        ?.takeIf { !containsTamilScript(it) }
                    if (recovered == null) null else recovered
                } else rawTitleBefore
                val m = if (rawMoralBefore != null && containsTamilScript(rawMoralBefore)) {
                    val recovered = translatePlainPhrase(rawMoralBefore, src, tgt)?.let { sanitizeTranslationText(it) }
                        ?.takeIf { !containsTamilScript(it) }
                    if (recovered == null) null else recovered
                } else rawMoralBefore
                (t ?: finalTitle) to (m ?: finalMoral)
            } else {
                (rawTitleBefore ?: finalTitle) to (rawMoralBefore ?: finalMoral)
            }

            val afterContent = sanitizeTranslationText(rawContent) ?: rawContent
            val beforeContent = rawContentBefore?.let { sanitizeTranslationText(it) ?: it } ?: afterContent

            TranslatedContent(
                title = finalTitle,
                content = afterContent,
                moral = finalMoral,
                titleBeforeParaphrase = beforeTitle,
                contentBeforeParaphrase = beforeContent,
                moralBeforeParaphrase = beforeMoral,
            )
        } catch (e: Exception) {
            log.warn("Translation JSON parse failed, extracting from raw: {}", e.message)
            val extracted = extractContentFromRaw(raw).ifBlank { raw }
            TranslatedContent(
                title = if (sourceLang == targetLang) sanitizeTranslationText(originalTitle) else null,
                content = sanitizeTranslationText(extracted) ?: extracted,
                moral = if (sourceLang == targetLang) sanitizeTranslationText(originalMoral) else null,
                titleBeforeParaphrase = null,
                contentBeforeParaphrase = null,
                moralBeforeParaphrase = null,
            )
        }
    }

    private fun normalizeJsonPayload(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            val firstNewline = text.indexOf('\n')
            if (firstNewline >= 0) {
                text = text.substring(firstNewline + 1)
            }
            text = text.trim()
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3).trim()
            }
        }
        if (text.startsWith("json", ignoreCase = true)) {
            val stripped = text.substring(4).trimStart()
            if (stripped.startsWith("{") || stripped.startsWith("[")) {
                text = stripped
            }
        }
        return text
    }

    private fun sanitizeTranslationText(text: String?): String? {
        if (text.isNullOrBlank()) return text
        var t = text.trim()
        t = Regex("^\\s*\\[[a-z]{2,5}]\\s*", RegexOption.IGNORE_CASE).replace(t, "")
        t = Regex(
            "^\\s*\"?(content|title|moral|content_before_paraphrase|title_before_paraphrase|moral_before_paraphrase)\"?:?\\s*[\"']?",
            RegexOption.IGNORE_CASE,
        ).replace(t, "")
        t = Regex("^[\"',;:\\s]+|[\"',;:\\s]+$").replace(t, "")
        return t.trim().takeIf { it.isNotBlank() }
    }

    private fun extractContentFromRaw(raw: String): String {
        val lower = raw.lowercase()
        val contentIdx = lower.indexOf("\"content\"")
        if (contentIdx >= 0) {
            val afterKey = raw.substring(contentIdx + 9).trimStart().removePrefix(":").trimStart()
            val start = afterKey.indexOf('"')
            if (start >= 0) {
                var i = start + 1
                val sb = StringBuilder()
                while (i < afterKey.length) {
                    val c = afterKey[i]
                    when {
                        c == '\\' && i + 1 < afterKey.length -> {
                            sb.append(afterKey[i + 1])
                            i += 2
                        }
                        c == '"' -> return sb.toString().trim()
                        else -> {
                            sb.append(c)
                            i++
                        }
                    }
                }
                return sb.toString().trim()
            }
        }
        return raw
    }
}
