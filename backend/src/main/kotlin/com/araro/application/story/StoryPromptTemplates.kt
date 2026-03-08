package com.araro.application.story

/**
 * Prompt templates for AI story generation with age-based vocabulary,
 * language-based cultural tuning (Tamil first), story length constraint,
 * and moral inclusion. Designed for structured JSON output.
 */
object StoryPromptTemplates {

    private const val SYSTEM_PREFIX = """You are a child-friendly story writer. You must respond with valid JSON only, no other text. Use this exact structure:
{"title": "...", "moral": "...", "story_text": "...", "estimated_duration": <number in minutes>}
Rules: No violence, weapons, politics, or scary content. Story must be uplifting and age-appropriate."""

    /** Age band for vocabulary: 1-4 simple, 5-7 medium, 8-12 richer. */
    fun vocabularyHint(age: Int): String = when {
        age <= 4 -> "Use very simple words and short sentences. No complex ideas."
        age <= 7 -> "Use clear, everyday words. Short to medium sentences."
        else -> "You may use a richer vocabulary; still suitable for children."
    }

    /** Tamil-first cultural tuning: settings, names, values. */
    fun culturalHint(language: String): String = when (language.lowercase()) {
        "ta", "tamil" -> "Set the story in a Tamil-friendly context: family, village or town, respect for elders, friendship, nature. Use culturally appropriate names and settings. Prefer Tamil cultural values (kindness, sharing, learning)."
        else -> "Keep the story culturally neutral and family-friendly."
    }

    /** Max words derived from reading time (e.g. 5 min at 150 wpm = 750). */
    fun lengthConstraint(maxWords: Int): String =
        "The story_text must be at most $maxWords words so it can be read in about 5 minutes. Do not exceed $maxWords words."

    /** Moral inclusion: always request a short, clear moral. */
    fun moralInstruction(): String =
        "Include a short, clear moral (one sentence) that the story teaches (e.g. sharing, honesty, kindness). Put it in the \"moral\" field."

    fun buildSystemMessage(): String = SYSTEM_PREFIX

    /** Main user prompt for structured story. */
    fun buildUserPrompt(
        age: Int,
        language: String,
        theme: String,
        childName: String,
        maxWords: Int
    ): String {
        val vocab = vocabularyHint(age)
        val culture = culturalHint(language)
        val length = lengthConstraint(maxWords)
        val moral = moralInstruction()
        return """
Write a short, child-friendly story in the language: $language.
Target age: $age years.
Theme: $theme.
Main character name: $childName.

$vocab
$culture
$length
$moral

Respond with only the JSON object (title, moral, story_text, estimated_duration). No markdown, no code block.
""".trimIndent()
    }

    /** Fallback prompt: simpler, no JSON if model fails to parse. */
    fun buildFallbackUserPrompt(
        age: Int,
        language: String,
        theme: String,
        childName: String,
        maxWords: Int
    ): String {
        val vocab = vocabularyHint(age)
        val length = lengthConstraint(maxWords)
        return """
Write a very short children's story in $language for age $age. Theme: $theme. Main character: $childName.
$vocab
$length
Reply with a JSON object with keys: title, moral, story_text, estimated_duration. Only valid JSON.
""".trimIndent()
    }
}
