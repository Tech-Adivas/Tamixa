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
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
        val theme: String? = null
    )

    /**
     * Transform story content with a custom prompt (LLM only, no TTS).
     * Used by "Regenerate with prompt" (edit page) to transform story content before the admin saves or submits.
     * When the LLM returns JSON (e.g. with story_text, title, moral), parses and extracts all fields
     * so title and moral are language-specific (same language as story_text).
     */
    fun transformContent(content: String, customPrompt: String): TransformResult {
        val trimmedPrompt = customPrompt.trim()
        if (trimmedPrompt.isBlank()) return TransformResult(content, null, null)
        val result = narrationLLM.transformWithCustomPrompt(content, trimmedPrompt, 8192)
        val raw = result.formattedText.trim().ifBlank { return TransformResult(content, null, null) }
        val parsed = extractAllFromJson(raw)
        if (parsed != null) return parsed
        log.warn("Regenerate JSON parse failed; raw length={} startsWith={}", raw.length, raw.take(80))
        return TransformResult(raw, null, null, null, null)
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
            TransformResult(storyText, title, moral, category, theme)
        } catch (e: Exception) {
            log.debug("JSON story parsing failed: {}", e.message)
            null
        }
    }

    /**
     * Prompt + TTS: transform story content with custom prompt via LLM, then build SSML and synthesize.
     */
    fun generatePromptAndTts(storyId: Long, language: String, customPrompt: String): ByteArray? {
        val ctx = getContentForLanguage(storyId, language) ?: return null
        val trimmedPrompt = customPrompt.trim()
        if (trimmedPrompt.isBlank()) return generateDirectTts(storyId, language)
        val result = narrationLLM.transformWithCustomPrompt(ctx.content, trimmedPrompt, 1024)
        val transformed = result.formattedText.trim().takeIf { it.isNotBlank() } ?: ctx.content
        val ssml = ssmlBuilder.buildSSML(transformed, ctx.language, ctx.age, ctx.toneMode)
        log.info("Admin prompt+TTS storyId={} lang={} promptLen={} outputLen={}", storyId, language, trimmedPrompt.length, transformed.length)
        return ttsService.synthesize(ssml, ctx.language, "default")
    }

    data class StoryContentContext(
        val content: String,
        val age: Int,
        val toneMode: ToneMode,
        val language: String
    )
}
