package com.tamixa.application.shortcontent

import com.tamixa.api.admin.dto.GeneratedShortContentItem
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.story.ModerationContext
import com.tamixa.application.story.StoryModerationService
import com.tamixa.domain.ShortContentType
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ShortContentGenerationService(
    private val openAI: OpenAIPort,
    private val objectMapper: ObjectMapper,
    private val storyModeration: StoryModerationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Generate short content items of the given type and language using AI.
     * Returns items that pass content moderation; rejected items are filtered out and counted.
     * Empty list if API key not set or generation fails.
     */
    fun generate(type: String, language: String, count: Int): ShortContentGenerationResult {
        if (count < 1 || count > 20) return ShortContentGenerationResult(emptyList(), 0)
        val canonicalType = ShortContentType.fromString(type)?.value ?: type.trim().uppercase()
        val lang = language.trim().lowercase()
        val langName = languageDisplayName(lang)
        val needsAnswer = canonicalType in setOf(
            ShortContentType.RIDDLE.value,
            ShortContentType.JOKE.value,
            ShortContentType.BRAIN_TEASER.value,
            ShortContentType.TRIVIA.value,
            ShortContentType.WORD_OF_THE_DAY.value
        )
        val systemPrompt = buildSystemPrompt(canonicalType, langName, needsAnswer)
        val userPrompt = "Generate exactly $count items. Return a JSON array. Each element must have \"content\"" +
            (if (needsAnswer) " and \"answer\" (brief answer or punchline)." else ".") +
            " Use only the requested language ($langName). Keep each content short (1-3 sentences)."
        val maxTokens = (count * 150).coerceIn(256, 2048)
        val raw = try {
            openAI.completeChat(systemPrompt, userPrompt, maxTokens)
        } catch (e: Exception) {
            log.warn("ShortContent generation failed type={} language={}: {}", canonicalType, lang, e.message)
            return ShortContentGenerationResult(emptyList(), 0)
        }
        if (raw.isBlank()) {
            log.warn("ShortContent generation returned empty response type={} language={}", canonicalType, lang)
            return ShortContentGenerationResult(emptyList(), 0)
        }
        val parsed: List<GeneratedShortContentItem> = parseResponse(raw, needsAnswer)
        return filterByModeration(parsed, canonicalType, lang)
    }

    private fun buildSystemPrompt(type: String, languageName: String, needsAnswer: Boolean): String {
        val typeDesc = when (type) {
            "RIDDLE" -> "riddles (a question or riddle, with a clear answer)"
            "THOUGHT_FOR_THE_DAY" -> "short inspirational thoughts or quotes for the day"
            "PROVERB" -> "traditional proverbs or sayings"
            "TONGUE_TWISTER" -> "tongue twisters (fun phrases that are hard to say fast)"
            "JOKE" -> "kid-friendly jokes (setup and punchline)"
            "FUN_FACT" -> "short fun facts (educational and interesting)"
            "WORD_OF_THE_DAY" -> "word of the day with a brief meaning or example"
            "BRAIN_TEASER" -> "simple brain teasers or logic puzzles with an answer"
            "AFFIRMATION" -> "positive affirmations for children"
            "QUOTE" -> "short inspirational quotes"
            "DID_YOU_KNOW" -> "\"Did you know?\" style facts"
            "RHYME" -> "short rhymes or verses"
            "TRIVIA" -> "trivia questions with answers"
            else -> "short kid-friendly content of type: $type"
        }
        return "You are a helpful assistant that creates kid-friendly, age-appropriate content in $languageName. " +
            "Generate $typeDesc. " +
            "Output ONLY a valid JSON array. No markdown, no code fence. " +
            "Each object must have \"content\" (string). " +
            (if (needsAnswer) "Include \"answer\" (string) for each item. " else "") +
            "Content must be safe for children and culturally appropriate."
    }

    private fun languageDisplayName(code: String): String = when (code) {
        "ta" -> "Tamil"
        "en" -> "English"
        "hi" -> "Hindi"
        "te" -> "Telugu"
        "kn" -> "Kannada"
        "ml" -> "Malayalam"
        "bn" -> "Bengali"
        else -> code
    }

    /**
     * Filters items through content moderation. Items that fail moderation are excluded;
     * rejectedCount is returned for auditing.
     */
    private fun filterByModeration(
        items: List<GeneratedShortContentItem>,
        type: String,
        language: String
    ): ShortContentGenerationResult {
        val context = ModerationContext(promptId = "short-content-gen-$type", language = language, age = 7)
        val passed = mutableListOf<GeneratedShortContentItem>()
        var rejected = 0
        for ((idx, item) in items.withIndex()) {
            val textToModerate = buildString {
                append(item.content)
                item.answer?.let { append(" ").append(it) }
            }
            try {
                storyModeration.moderateBeforeSave(textToModerate, context.copy(promptId = "short-content-gen-$type-$idx"))
                passed.add(item)
            } catch (e: ContentModerationException) {
                rejected++
                log.warn("ShortContent item rejected by moderation idx={} type={} lang={}: {}", idx, type, language, e.message)
            }
        }
        return ShortContentGenerationResult(passed, rejected)
    }

    private fun parseResponse(raw: String, expectAnswer: Boolean): List<GeneratedShortContentItem> {
        val trimmed = raw.trim().removeSurrounding("```json").removeSurrounding("```").trim()
        return try {
            val tree = objectMapper.readTree(trimmed)
            if (!tree.isArray) {
                log.warn("ShortContent generation response was not a JSON array")
                return emptyList()
            }
            tree.mapNotNull { node ->
                if (!node.isObject) return@mapNotNull null
                val content = node.get("content")?.asText()?.trim() ?: return@mapNotNull null
                if (content.isBlank()) return@mapNotNull null
                val answer = node.get("answer")?.asText()?.trim()?.takeIf { it.isNotBlank() }
                GeneratedShortContentItem(content = content, answer = answer)
            }
        } catch (e: Exception) {
            log.warn("ShortContent generation parse failed: {}", e.message)
            emptyList()
        }
    }
}

/** Result of short content generation with moderation filtering. */
data class ShortContentGenerationResult(
    val items: List<GeneratedShortContentItem>,
    val rejectedCount: Int
)
