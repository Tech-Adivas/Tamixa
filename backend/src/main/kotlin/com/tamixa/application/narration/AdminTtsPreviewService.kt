package com.tamixa.application.narration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.storylibrary.StoryCategories
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.domain.narration.ToneMode
import com.tamixa.application.narration.EmotionToToneMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Admin-only: generate TTS audio for a story either directly (content as-is) or
 * after transforming content with a custom LLM prompt. Used for preview and experimentation.
 */
@Service
class AdminTtsPreviewService(
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val ssmlBuilder: SSMLBuilderService,
    private val ttsService: TTSService,
    private val narrationLLM: NarrationLLMPort,
    private val objectMapper: ObjectMapper,
    @Value("\${app.narration.regenerate-min-words:900}") private val regenerateMinWords: Int = 900
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val pauseMarkerRegex = Regex("""\[\s*Pause\s*\d+(?:ms|s)\s*]""", RegexOption.IGNORE_CASE)
    private val refusalRegex = Regex(
        """(?is)\b(i\s*(?:am|'m)\s*sorry|cannot|can't|won't|unable to)\b.*\b(assist|help|comply|provide|request)\b"""
    )

    /**
     * Get story content for the given language: translation content if exists, else master story content.
     * Returns null if story not found. Also returns age and toneMode for SSML.
     */
    fun getContentForLanguage(storyId: Long, language: String): StoryContentContext? {
        val story = storyLibraryRepository.findById(storyId) ?: return null
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
        val content = (translation?.content?.trim()?.takeIf { it.isNotBlank() } ?: story.content.trim())
            .takeIf { it.isNotBlank() } ?: return null
        val toneMode = EmotionToToneMapper.toToneMode(story.emotionMode)
        return StoryContentContext(
            content = content,
            age = story.age.coerceIn(1, 12),
            toneMode = toneMode,
            language = effectiveLang
        )
    }

    /**
     * Direct TTS: build SSML from story content as-is and synthesize. No LLM rewrite.
     */
    fun generateDirectTts(storyId: Long, language: String): ByteArray? {
        val ctx = getContentForLanguage(storyId, language) ?: return null
        val ssml = ssmlBuilder.buildSSML(ctx.content, ctx.language, ctx.age, ctx.toneMode)
        log.info("Admin direct TTS storyId={} lang={} contentLen={}", storyId, language, ctx.content.length)
        return ttsService.synthesize(ssml, ctx.language, "default")
    }

    data class TransformResult(
        val content: String,
        val title: String?,
        val moral: String?,
        val category: String? = null,
        val theme: String? = null,
        /** Up to 10 prompts, max 400 chars each; shown in apps for library playback. */
        val parentDiscussionPrompts: List<String>? = null,
        val parentContentNote: String? = null,
        val speakAlongPrompt: String? = null,
    )

    /**
     * Transform story content with a custom prompt (LLM only, no TTS).
     * Used by "Regenerate with prompt" (edit page) to transform story content before the admin saves or submits.
     * When the LLM returns JSON (e.g. with story_text, title, moral), parses and extracts all fields
     * so title and moral are language-specific (same language as story_text).
     */
    fun transformContent(content: String, customPrompt: String, languageHint: String? = null): TransformResult {
        val trimmedPrompt = customPrompt.trim()
        if (trimmedPrompt.isBlank()) return TransformResult(content, null, null)
        val result = narrationLLM.transformWithCustomPrompt(content, trimmedPrompt, 8192)
        val raw = result.formattedText.trim().ifBlank { return TransformResult(content, null, null) }
        if (isLikelyModelRefusal(raw)) {
            throw IllegalArgumentException("Regeneration was blocked by the language model for this input. Please edit and retry.")
        }
        val parsed = extractAllFromJson(raw)
        if (parsed != null) {
            val repaired = ensureNativeStorytelling(parsed.content, languageHint, content, force = true)
            if (isLikelyModelRefusal(repaired)) {
                throw IllegalArgumentException("Regeneration was blocked by the language model for this input. Please edit and retry.")
            }
            return parsed.copy(content = repaired)
        }
        log.warn("Regenerate JSON parse failed; raw length={} startsWith={}", raw.length, raw.take(80))
        val repaired = ensureNativeStorytelling(raw, languageHint, content, force = true)
        if (isLikelyModelRefusal(repaired)) {
            throw IllegalArgumentException("Regeneration was blocked by the language model for this input. Please edit and retry.")
        }
        return TransformResult(
            repaired,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        )
    }

    private fun isLikelyModelRefusal(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 320) return false
        return refusalRegex.containsMatchIn(trimmed)
    }

    /**
     * If the LLM response looks like JSON, extract story_text, title, moral, category, theme.
     * Category is normalized to canonical app categories when present.
     */
    private fun extractAllFromJson(raw: String): TransformResult? {
        var trimmed = raw.trim()
            .replace(Regex("^```\\s*json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "")
            .trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            val tree: JsonNode = objectMapper.readTree(trimmed) ?: return null
            val storyText = tree.path("story_text").asText("").ifBlank { tree.path("storyText").asText("") }.trim()
            if (storyText.isBlank()) return null
            val title = tree.path("title").asText("").trim().takeIf { it.isNotBlank() }
            val moral = tree.path("moral").asText("").trim().takeIf { it.isNotBlank() }
            val rawCategory = tree.path("category").asText("").trim().takeIf { it.isNotBlank() }
            val category = rawCategory?.let { StoryCategories.toCanonical(it) }
            val theme = tree.path("theme").asText("").trim().takeIf { it.isNotBlank() }
            val parentDiscussionPrompts = extractParentDiscussionPrompts(tree)
            val parentContentNote = extractParentContentNote(tree)
            val speakAlongPrompt = extractSpeakAlongPrompt(tree)
            TransformResult(
                storyText,
                title,
                moral,
                category,
                theme,
                parentDiscussionPrompts,
                parentContentNote,
                speakAlongPrompt,
            )
        } catch (e: Exception) {
            log.debug("JSON story parsing failed: {}", e.message)
            null
        }
    }

    private fun extractParentDiscussionPrompts(tree: JsonNode): List<String>? {
        val node = tree.get("parent_discussion_prompts") ?: tree.get("parentDiscussionPrompts")
        if (node == null || node.isNull || !node.isArray) return null
        return node.mapNotNull { el ->
            if (el.isNull) null
            else el.asText("").trim().takeIf { it.isNotBlank() }?.take(PARENT_DISCUSSION_PROMPT_MAX_LEN)
        }.take(PARENT_DISCUSSION_PROMPTS_MAX_COUNT)
    }

    private fun extractParentContentNote(tree: JsonNode): String? {
        val node = tree.get("parent_content_note") ?: tree.get("parentContentNote") ?: return null
        if (node.isNull) return null
        return node.asText("").trim().take(PARENT_CONTENT_NOTE_MAX_LEN).takeIf { it.isNotBlank() }
    }

    private fun extractSpeakAlongPrompt(tree: JsonNode): String? {
        val node = tree.get("speak_along_prompt") ?: tree.get("speakAlongPrompt") ?: return null
        if (node.isNull) return null
        return node.asText("").trim().take(SPEAK_ALONG_PROMPT_MAX_LEN).takeIf { it.isNotBlank() }
    }

    companion object {
        private const val PARENT_DISCUSSION_PROMPTS_MAX_COUNT = 10
        private const val PARENT_DISCUSSION_PROMPT_MAX_LEN = 400
        private const val PARENT_CONTENT_NOTE_MAX_LEN = 4000
        private const val SPEAK_ALONG_PROMPT_MAX_LEN = 500
    }

    /**
     * Prompt + TTS: transform story content with custom prompt via LLM, then build SSML and synthesize.
     */
    fun generatePromptAndTts(storyId: Long, language: String, customPrompt: String): ByteArray? {
        val ctx = getContentForLanguage(storyId, language) ?: return null
        val trimmedPrompt = customPrompt.trim()
        if (trimmedPrompt.isBlank()) return generateDirectTts(storyId, language)
        val result = narrationLLM.transformWithCustomPrompt(ctx.content, trimmedPrompt, 1024)
        val transformed = ensureNativeStorytelling(
            result.formattedText.trim().takeIf { it.isNotBlank() } ?: ctx.content,
            ctx.language,
            ctx.content,
            force = true
        )
        val ssml = ssmlBuilder.buildSSML(transformed, ctx.language, ctx.age, ctx.toneMode)
        log.info("Admin prompt+TTS storyId={} lang={} promptLen={} outputLen={}", storyId, language, trimmedPrompt.length, transformed.length)
        return ttsService.synthesize(ssml, ctx.language, "default")
    }

    /** Apply native storytelling repair for admin regenerate flow when output is still literal/bookish. */
    fun ensureNativeStorytelling(
        content: String,
        languageHint: String?,
        sourceContent: String? = null,
        force: Boolean = false
    ): String {
        val cleaned = sanitizeNarrationArtifacts(content)
        val language = languageHint?.trim()?.lowercase()?.take(10).orEmpty()
        if (!force && !needsNativeRepair(cleaned, language, sourceContent)) return cleaned
        val prompt = buildNativeRepairPrompt(language, sourceContent, cleaned)
        return try {
            val repaired = narrationLLM.transformWithCustomPrompt(cleaned, prompt, 4096).formattedText
            var normalized = sanitizeNarrationArtifacts(repaired).ifBlank { cleaned }
            // Expansion can still under-shoot word target on some model runs; retry a few times.
            repeat(3) {
                if (!needsLengthExpansion(normalized, sourceContent)) return@repeat
                val expansionPrompt = buildLengthExpansionPrompt(language, sourceContent, normalized)
                val expanded = narrationLLM.transformWithCustomPrompt(normalized, expansionPrompt, 6144).formattedText
                normalized = sanitizeNarrationArtifacts(expanded).ifBlank { normalized }
            }
            normalized
        } catch (e: Exception) {
            log.warn("Native storytelling repair skipped lang={} err={}", language, e.message)
            cleaned
        }
    }

    /**
     * Aggressively expand regenerate output to meet the configured long-form minimum.
     * Uses native repair + multiple expansion rounds while preserving plot continuity.
     */
    data class RegenerateExpansionResult(
        val content: String,
        val finalWords: Int,
        val metMinimum: Boolean,
        val attempts: Int
    )

    fun expandToMinimumWords(
        content: String,
        languageHint: String?,
        sourceContent: String? = null,
        minimumWords: Int = regenerateMinWordsTarget(),
        maxAttempts: Int = 3
    ): RegenerateExpansionResult {
        val minWords = minimumWords.coerceIn(300, 2000)
        val language = languageHint?.trim()?.lowercase()?.take(10).orEmpty()
        val source = sourceContent?.trim().orEmpty().ifBlank { content }
        var candidate = sanitizeNarrationArtifacts(content).ifBlank { source }
        var attempts = 0
        if (wordCount(candidate) >= minWords) {
            return RegenerateExpansionResult(candidate, wordCount(candidate), metMinimum = true, attempts = attempts)
        }

        candidate = ensureNativeStorytelling(candidate, language, source, force = true)
        attempts += 1
        if (wordCount(candidate) >= minWords) {
            return RegenerateExpansionResult(candidate, wordCount(candidate), metMinimum = true, attempts = attempts)
        }

        repeat(maxAttempts.coerceIn(1, 6)) {
            val prompt = buildAggressiveExpansionPrompt(
                language = language,
                sourceContent = source,
                currentContent = candidate,
                minimumWords = minWords
            )
            val expanded = try {
                narrationLLM.transformWithCustomPrompt(candidate, prompt, 8192).formattedText
            } catch (e: Exception) {
                log.warn("Regenerate expansion attempt failed lang={} err={}", language, e.message)
                attempts += 1
                return@repeat
            }
            attempts += 1
            val cleaned = sanitizeNarrationArtifacts(expanded).ifBlank { candidate }
            if (!isLikelyModelRefusal(cleaned) && cleaned.isNotBlank()) {
                candidate = cleaned
            }
            if (wordCount(candidate) >= minWords) {
                return RegenerateExpansionResult(candidate, wordCount(candidate), metMinimum = true, attempts = attempts)
            }
        }
        return RegenerateExpansionResult(candidate, wordCount(candidate), metMinimum = false, attempts = attempts)
    }

    private fun sanitizeNarrationArtifacts(text: String): String {
        if (text.isBlank()) return text
        val withoutFence = text.trim()
            .replace(Regex("^```\\s*plaintext\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*text\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "")
        return withoutFence
            .replace(Regex("""\bnn\b""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""[^\S\n]{2,}"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun needsNativeRepair(text: String, language: String, sourceContent: String?): Boolean {
        if (text.isBlank()) return false
        if (Regex("""\bnn\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) return true
        val paragraphs = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.size < 4) return true
        if (!pauseMarkerRegex.containsMatchIn(text)) return true
        if (needsLengthExpansion(text, sourceContent)) return true
        return when (language) {
            "ta", "tamil" -> text.count { it in '\u0B80'..'\u0BFF' } < 80
            "hi", "hindi" -> text.count { it in '\u0900'..'\u097F' } < 80
            "te", "telugu" -> text.count { it in '\u0C00'..'\u0C7F' } < 80
            "kn", "kannada" -> text.count { it in '\u0C80'..'\u0CFF' } < 80
            "ml", "malayalam" -> text.count { it in '\u0D00'..'\u0D7F' } < 80
            else -> false
        }
    }

    private fun needsLengthExpansion(current: String, sourceContent: String?): Boolean {
        val srcWords = countWords(sourceContent.orEmpty())
        val outWords = countWords(current)
        val configuredMin = regenerateMinWords.coerceIn(300, 2000)
        if (srcWords < 600) return outWords < configuredMin
        val minAllowed = (srcWords * 0.7).toInt().coerceAtLeast(configuredMin)
        return outWords < minAllowed
    }

    private fun countWords(text: String): Int =
        text.split(Regex("\\s+")).count { it.isNotBlank() }

    /** Minimum target words for admin regenerate flow (config-driven, bounded). */
    fun regenerateMinWordsTarget(): Int = regenerateMinWords.coerceIn(300, 2000)

    /** Public utility for callers that need consistent word counting. */
    fun wordCount(text: String): Int = countWords(text)

    private fun buildNativeRepairPrompt(language: String, sourceContent: String?, currentContent: String): String {
        val languageLabel = when (language) {
            "ta", "tamil" -> "Tamil"
            "hi", "hindi" -> "Hindi"
            "te", "telugu" -> "Telugu"
            "kn", "kannada" -> "Kannada"
            "ml", "malayalam" -> "Malayalam"
            "en", "english" -> "English"
            else -> "the same language as input"
        }
        val scriptRule = when (language) {
            "ta", "tamil" -> "Use Tamil script consistently."
            "hi", "hindi" -> "Use Devanagari script consistently."
            "te", "telugu" -> "Use Telugu script consistently."
            "kn", "kannada" -> "Use Kannada script consistently."
            "ml", "malayalam" -> "Use Malayalam script consistently."
            else -> "Keep script/language consistent throughout."
        }
        val srcWords = countWords(sourceContent.orEmpty())
        val outWords = countWords(currentContent)
        val lengthRule = if (srcWords >= 600) {
            val minWords = (srcWords * 0.7).toInt().coerceAtLeast(700)
            val maxWords = (srcWords * 1.15).toInt().coerceAtMost(1800)
            "Length fidelity: source is long ($srcWords words), so rewritten output must stay rich and detailed (target range: $minWords-$maxWords words). Do NOT collapse to a short summary."
        } else {
            val configuredMin = regenerateMinWords.coerceIn(300, 2000)
            val creativeTargetMax = (configuredMin + 250).coerceAtMost(1900)
            "Expand this into a rich long-form narration (target range: $configuredMin-$creativeTargetMax words) while preserving the same plot and moral."
        }
        return """
Rewrite this story text to sound native and orally narrated.

Write fluent spoken $languageLabel storytelling with natural rhythm and native phrasing.
$scriptRule
$lengthRule

Delivery format (mandatory):
- Use short breath-friendly lines (one short idea per line).
- Prefer oral cadence over written/literary prose.
- Keep transitions smooth and emotional.
- Keep or add meaningful pause markers at natural beats.

Must keep:
- Same plot, characters, and moral
- Same marker style ([Warm tone], [Pause 500ms], etc.) in English labels
- Emotional arc and continuity

Must improve:
- Natural spoken phrasing (not literal translation)
- Smoother dialogue
- Remove broken artifacts like "nn"
- Keep strong ending and audio-friendly rhythm
- Add child-safe imagination: vivid sensory images, emotional micro-moments, and scene-by-scene details that make the world feel alive.
- Enrich through creative narration and short natural dialogue, without changing the core plot outcomes.

Current output is about $outWords words. Expand if needed to meet length fidelity while preserving plot.

Output only the rewritten story text.
        """.trimIndent()
    }

    private fun buildLengthExpansionPrompt(language: String, sourceContent: String?, currentContent: String): String {
        val srcWords = countWords(sourceContent.orEmpty())
        val currentWords = countWords(currentContent)
        val configuredMin = regenerateMinWords.coerceIn(300, 2000)
        val targetMin = if (srcWords >= 600) {
            (srcWords * 0.8).toInt().coerceAtLeast(configuredMin)
        } else {
            configuredMin
        }
        val targetMax = if (srcWords >= 600) {
            (srcWords * 1.15).toInt().coerceAtMost(1900)
        } else {
            (targetMin + 250).coerceAtMost(1900)
        }
        val languageLabel = when (language) {
            "ta", "tamil" -> "Tamil"
            "hi", "hindi" -> "Hindi"
            "te", "telugu" -> "Telugu"
            "kn", "kannada" -> "Kannada"
            "ml", "malayalam" -> "Malayalam"
            "en", "english" -> "English"
            else -> "the same language"
        }
        return """
The current rewritten story is too short ($currentWords words) compared to source ($srcWords words).

Expand this story in fluent spoken $languageLabel storytelling style.

Strict rules:
- Keep the SAME plot order, characters, and moral.
- Do not invent new major events or new characters.
- Add natural dialogue, sensory details, emotional transitions, and scene bridges.
- Increase imagination and creativity in narration: playful phrasing, wonder-filled visual details, and emotionally warm storytelling suitable for children.
- Keep marker labels in English (e.g. [Warm tone], [Pause 500ms]).
- Keep script/language consistent.
- Keep oral rhythm: short, breath-friendly lines; avoid stiff long written sentences.
- Target length: $targetMin-$targetMax words.

Return only the expanded story text.
        """.trimIndent()
    }

    private fun buildAggressiveExpansionPrompt(
        language: String,
        sourceContent: String,
        currentContent: String,
        minimumWords: Int
    ): String {
        val currentWords = countWords(currentContent)
        val sourceWords = countWords(sourceContent)
        val targetMax = (minimumWords + 250).coerceAtMost(1900)
        val languageLabel = when (language) {
            "ta", "tamil" -> "Tamil"
            "hi", "hindi" -> "Hindi"
            "te", "telugu" -> "Telugu"
            "kn", "kannada" -> "Kannada"
            "ml", "malayalam" -> "Malayalam"
            "en", "english" -> "English"
            else -> "the same language"
        }
        return """
You are expanding a children's story narration for spoken audio.

Current draft is too short: $currentWords words.
Source context length: $sourceWords words.

Rewrite and EXPAND the current draft to fluent spoken $languageLabel storytelling with:
- rich sensory imagery,
- child-safe imagination and wonder,
- short natural dialogues,
- emotional transitions and scene bridges.

Hard constraints:
- Keep same core plot, characters, and moral.
- Do not add new major characters or change ending outcome.
- Keep language/script consistent.
- Keep marker labels in English if present.
- Target length must be between $minimumWords and $targetMax words.

Return only the expanded story text.
        """.trimIndent()
    }

    data class StoryContentContext(
        val content: String,
        val age: Int,
        val toneMode: ToneMode,
        val language: String
    )
}
