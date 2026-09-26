package com.tamixa.infrastructure.translation

import com.tamixa.application.port.TranslationClientPort
import com.tamixa.application.port.TranslatedContent
import com.tamixa.infrastructure.http.ApiErrorExtractor
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Real translation via OpenAI chat API.
 * Translates Tamil (and other languages) to target language.
 * Enable with app.translation.provider=openai and OPENAI_API_KEY set.
 * When [app.translation.openai-fallback-to-gemini] is true, use [OpenAiWithGeminiFallbackTranslationClient] instead.
 */
@Component
@Conditional(OnTranslationOpenAiOnlyCondition::class)
class OpenAITranslationClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.openai.model:gpt-4o-mini}") private val model: String,
    @Value("\${app.translation-pipeline.translation-timeout-ms:30000}") private val defaultTimeoutMs: Long,
    @Value("\${app.story.max-words:900}") private val maxStoryWords: Int
) : TranslationClientPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val langNames = mapOf(
        "ta" to "Tamil",
        "en" to "English",
        "hi" to "Hindi",
        "kn" to "Kannada",
        "te" to "Telugu",
        "ml" to "Malayalam",
        "bn" to "Bengali"
    )

    override fun translate(sourceLang: String, targetLang: String, text: String, timeoutMs: Long): String {
        val result = translateStructured(
            sourceLang, targetLang, null, text, null, timeoutMs,
            null, null, null,
        )
        return result.content
    }

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [IllegalStateException::class],
        maxAttempts = 2,
        backoff = Backoff(delay = 1000, multiplier = 1.5)
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
    ): TranslatedContent {
        log.info("OpenAI translation start {}->{} contentLen={}", sourceLang, targetLang, content.length)
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY for translation.")
        }
        val srcName = langNames[sourceLang] ?: sourceLang
        val tgtName = langNames[targetLang] ?: targetLang
        val wordCap = maxStoryWords.coerceIn(200, 2500)
        val hasParent = hasParentSourceForTranslation(parentContentNote, parentDiscussionPrompts, speakAlongPrompt)
        val systemPrompt = """
            You are a translator and storyteller for children's stories. Translate from $srcName to $tgtName.
            Output valid JSON only with keys:
              title (or null),
              content,
              moral (or null),
              title_before_paraphrase (or null),
              content_before_paraphrase,
              moral_before_paraphrase (or null)${if (hasParent) ",\n              parent_content_note (or null),\n              parent_discussion_prompts (JSON array or null),\n              speak_along_prompt (or null)" else ""}.
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
            ${if (hasParent) parentFacingKeysSystemPromptBlock(tgtName) else ""}
        """.trimIndent()
        val parts = buildList {
            title?.let { add("TITLE: $it") }
            add("CONTENT:\n$content")
            moral?.let { add("MORAL: $it") }
        }
        val parentAppendix = buildParentFacingUserAppendix(parentContentNote, parentDiscussionPrompts, speakAlongPrompt)
        val keyList = buildString {
            append("\"title\", \"content\", \"moral\", \"title_before_paraphrase\", \"content_before_paraphrase\", \"moral_before_paraphrase\"")
            if (hasParent) {
                append(", \"parent_content_note\", \"parent_discussion_prompts\", \"speak_along_prompt\"")
            }
        }
        val userPrompt =
            "Translate the following story. Return JSON with keys exactly: $keyList (use null only if the source truly had no title/moral${if (hasParent) " or no parent-facing field" else ""}). All string values in $tgtName.\n\n${parts.joinToString("\n\n")}" +
                (if (parentAppendix != null) "\n\n$parentAppendix" else "")
        val rawCompletion = completeChat(systemPrompt, userPrompt, maxTokens = 12288, temperature = 0.5)
        val result = fillMissingTitleMoral(
            parseTranslationResponse(
                rawCompletion,
                title,
                content,
                moral,
                sourceLang,
                targetLang,
                parentContentNote,
                parentDiscussionPrompts,
                speakAlongPrompt,
            ),
            originalTitle = title,
            originalMoral = moral,
            sourceLang = sourceLang,
            targetLang = targetLang
        )
        log.info("OpenAI translation success {}->{} resultLen={}", sourceLang, targetLang, result.content.length)
        return result
    }

    private fun completeChat(systemPrompt: String, userPrompt: String, maxTokens: Int, temperature: Double): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY for translation.")
        }
        val request = ChatRequest(
            model = model.trim().ifBlank { "gpt-4o-mini" },
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            maxTokens = maxTokens.coerceIn(64, 16384),
            temperature = temperature.coerceIn(0.0, 1.0)
        )
        val headers = HttpHeaders().apply {
            setBearerAuth(apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(request, headers)
        val url = "$baseUrl/v1/chat/completions"
        val response = try {
            restTemplate.postForObject(url, entity, ChatResponse::class.java)
        } catch (e: Exception) {
            val msg = ApiErrorExtractor.extract(e, objectMapper)
            throw IllegalStateException("Translation failed: $msg", e)
        } ?: throw IllegalStateException("Empty translation response")
        return response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalStateException("No content in translation response")
    }

    /**
     * Models sometimes omit title/moral in large JSON (especially for te/kn/ml). Fill with a short second call.
     */
    private fun fillMissingTitleMoral(
        parsed: TranslatedContent,
        originalTitle: String?,
        originalMoral: String?,
        sourceLang: String,
        targetLang: String
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
            titleBeforeParaphrase = parsed.titleBeforeParaphrase,
            contentBeforeParaphrase = parsed.contentBeforeParaphrase,
            moralBeforeParaphrase = parsed.moralBeforeParaphrase,
            parentContentNote = parsed.parentContentNote,
            parentDiscussionPrompts = parsed.parentDiscussionPrompts,
            speakAlongPrompt = parsed.speakAlongPrompt,
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
            completeChat(system, phrase.trim(), maxTokens = 256, temperature = 0.15)
                .lines()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
        } catch (e: Exception) {
            log.warn("Plain phrase translation failed {}->{}: {}", src, tgt, e.message)
            null
        }
    }

    /** Case-insensitive JSON keys (models vary: Title vs title). */
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

    /** Tamil Unicode block U+0B80–U+0BFF. When translating FROM Tamil TO another language, title/moral must NOT contain Tamil. */
    private fun containsTamilScript(text: String?): Boolean =
        text != null && Regex("[\u0B80-\u0BFF]").containsMatchIn(text)

    private fun recordTamilLeakRecovery(field: String, targetLang: String, success: Boolean) {
        meterRegistry.counter(
            "translation_tamil_leak_recovery_total",
            "field", field.ifBlank { "unknown" }.lowercase(),
            "target", targetLang.ifBlank { "unknown" }.lowercase(),
            "outcome", if (success) "success" else "failure"
        ).increment()
    }

    private fun parseTranslationResponse(
        raw: String,
        originalTitle: String?,
        originalContent: String,
        originalMoral: String?,
        sourceLang: String,
        targetLang: String,
        originalParentContentNote: String?,
        originalParentDiscussionPrompts: List<String>?,
        originalSpeakAlong: String?,
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
                "translated_content"
            )
            val rawTitle = sanitizeTranslationText(jsonTextField(node, "title", "heading"))
            val rawMoral = sanitizeTranslationText(jsonTextField(node, "moral", "lesson"))
            val rawTitleBefore = sanitizeTranslationText(
                jsonTextField(node, "title_before_paraphrase", "titleBeforeParaphrase")
            )
            val rawMoralBefore = sanitizeTranslationText(
                jsonTextField(node, "moral_before_paraphrase", "moralBeforeParaphrase")
            )
            val src = sourceLang.trim().lowercase()
            val tgt = targetLang.trim().lowercase()
            // When translating FROM Tamil TO another language: if title/moral still contain Tamil, retry a short phrase translation.
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
                            targetLang
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
                            targetLang
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

            // Before-paraphrase snapshot: same recovery rules (only matters for Tamil->other when leaks occur).
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

            val parentFacing = if (hasParentSourceForTranslation(
                    originalParentContentNote,
                    originalParentDiscussionPrompts,
                    originalSpeakAlong,
                )
            ) {
                mergeParentFacingWithSourceTarget(
                    parseParentFacingFromTranslationJson(node),
                    sourceLang,
                    targetLang,
                    originalParentContentNote,
                    originalParentDiscussionPrompts,
                    originalSpeakAlong,
                )
            } else {
                ParsedParentFacingFields(null, null, null)
            }

            TranslatedContent(
                title = finalTitle,
                content = afterContent,
                moral = finalMoral,
                titleBeforeParaphrase = beforeTitle,
                contentBeforeParaphrase = beforeContent,
                moralBeforeParaphrase = beforeMoral,
                parentContentNote = parentFacing.parentContentNote,
                parentDiscussionPrompts = parentFacing.parentDiscussionPrompts,
                speakAlongPrompt = parentFacing.speakAlongPrompt,
            )
        } catch (e: Exception) {
            log.warn("Translation JSON parse failed, extracting from raw: {}", e.message)
            val extracted = extractContentFromRaw(raw).ifBlank { raw }
            val src = sourceLang.trim().lowercase()
            val tgt = targetLang.trim().lowercase()
            val same = src == tgt
            val pf = if (same && hasParentSourceForTranslation(
                    originalParentContentNote,
                    originalParentDiscussionPrompts,
                    originalSpeakAlong,
                )
            ) {
                ParsedParentFacingFields(
                    parentContentNote = originalParentContentNote?.trim()?.takeIf { it.isNotBlank() },
                    parentDiscussionPrompts = originalParentDiscussionPrompts
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.takeIf { it.isNotEmpty() },
                    speakAlongPrompt = originalSpeakAlong?.trim()?.takeIf { it.isNotBlank() },
                )
            } else {
                ParsedParentFacingFields(null, null, null)
            }
            TranslatedContent(
                title = if (same) sanitizeTranslationText(originalTitle) else null,
                content = sanitizeTranslationText(extracted) ?: extracted,
                moral = if (same) sanitizeTranslationText(originalMoral) else null,
                titleBeforeParaphrase = null,
                contentBeforeParaphrase = null,
                moralBeforeParaphrase = null,
                parentContentNote = pf.parentContentNote,
                parentDiscussionPrompts = pf.parentDiscussionPrompts,
                speakAlongPrompt = pf.speakAlongPrompt,
            )
        }
    }

    /**
     * Normalize model outputs into a pure JSON payload.
     * Handles common wrappers like:
     * - ```json\n{...}\n```
     * - ```\n{...}\n```
     * - json\n{...}
     */
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

    /** Removes labels (content:, title:, moral:), "[lang]" prefixes, and stray quotes/commas from translation output. */
    private fun sanitizeTranslationText(text: String?): String? {
        if (text.isNullOrBlank()) return text
        var t = text.trim()
        t = Regex("^\\s*\\[[a-z]{2,5}]\\s*", RegexOption.IGNORE_CASE).replace(t, "")
        t = Regex(
            "^\\s*\"?(content|title|moral|content_before_paraphrase|title_before_paraphrase|moral_before_paraphrase)\"?:?\\s*[\"']?",
            RegexOption.IGNORE_CASE
        ).replace(t, "")
        t = Regex("^[\"',;:\\s]+|[\"',;:\\s]+$").replace(t, "")
        return t.trim().takeIf { it.isNotBlank() }
    }

    /** When JSON parse fails, try to extract story content from raw response (after "content" or before trailing }). */
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
                        else -> { sb.append(c); i++ }
                    }
                }
                return sb.toString().trim()
            }
        }
        return raw
    }

    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @JsonProperty("max_tokens") val maxTokens: Int,
        @JsonProperty("temperature") val temperature: Double = 0.0
    )

    private data class ChatMessage(val role: String, val content: String)

    private data class ChatResponse(
        val choices: List<Choice>?,
        val usage: Usage?
    )

    private data class Choice(val message: Message?)

    private data class Message(val content: String?)

    private data class Usage(
        @JsonProperty("prompt_tokens") val promptTokens: Int,
        @JsonProperty("completion_tokens") val completionTokens: Int,
        @JsonProperty("total_tokens") val totalTokens: Int
    )
}
