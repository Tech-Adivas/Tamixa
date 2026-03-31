package com.tamixa.application.story

/**
 * Prompt templates for AI story generation. Aligned with Tamixa storyteller persona:
 * warm, narration-friendly, ~900 words, short paragraphs, structured JSON.
 * Use [StoryPromptBuilder] for the canonical prompts used by the app.
 */
object StoryPromptTemplates {

    const val FULL_LENGTH_WORDS_MIN: Int = 850
    const val FULL_LENGTH_WORDS_MAX: Int = 950
    const val FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN: Int = 420
    const val FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MAX: Int = 480

    private val SYSTEM_PREFIX = """You are Tamixa, an expert storyteller who creates warm, engaging, narration-friendly stories for children and families. Generate a story in the language specified in the user request. Use simple, clear language; short paragraphs (1–3 sentences); tone warm and pleasant. Tell one continuous story: logical order, no missing scenes or unexplained jumps. Conversational spoken style suitable for TTS (~7–8 minutes when full length: roughly ${FULL_LENGTH_WORDS_MIN}–${FULL_LENGTH_WORDS_MAX} words at natural pacing). Optional light voice markers in story_text when they help listening (e.g. [Pause 500ms], [Warm tone])—do not overuse. Positive ending; one short moral. Return only JSON: title, category, theme, moral, story_text, estimated_duration_seconds (number, e.g. ${FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN}–${FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MAX}). No other text."""

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

    /** Max words: ${FULL_LENGTH_WORDS_MIN}–${FULL_LENGTH_WORDS_MAX} for ~7–8 min narration at natural pacing. */
    fun lengthConstraint(maxWords: Int): String =
        "Target story length (full-length): approximately ${FULL_LENGTH_WORDS_MIN} to ${FULL_LENGTH_WORDS_MAX} words (about 7–8 minutes of narration). For this request, keep it within the configured maximum: do not exceed $maxWords words."

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
Generate a story in $language based on this request: for $childName (age $age), about $theme.

$vocab
$culture
$length
$moral

Respond with only the JSON object: title, category, theme, moral, story_text, estimated_duration_seconds. No markdown, no code block.
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
Generate a story in $language for $childName (age $age) about $theme. $vocab $length
Reply with a JSON object: title, category, theme, moral, story_text, estimated_duration_seconds. Only valid JSON.
""".trimIndent()
    }
}
