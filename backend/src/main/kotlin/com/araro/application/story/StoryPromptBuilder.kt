package com.araro.application.story

import org.springframework.stereotype.Component

/**
 * Builds story generation prompts with strict safety rules. Use only with
 * [SanitizedInput] from [StorySafetyMiddleware]; never concatenate raw user input.
 *
 * Rules:
 * - Age-based vocabulary and max word count
 * - Force moral inclusion and positive tone
 * - Block political, violent, religious conflict topics (enforced in system prompt)
 * - Tamil cultural alignment when language=ta
 * - Structured JSON output with estimated_duration_seconds
 */
@Component
class StoryPromptBuilder {

    private val allowedLanguages = setOf("en", "ta", "tamil", "hi", "te", "kn", "ml")
    private val maxWordsByAge = mapOf(
        1 to 150, 2 to 200, 3 to 250, 4 to 300,
        5 to 400, 6 to 500, 7 to 550,
        8 to 600, 9 to 650, 10 to 700, 11 to 750, 12 to 750
    )

    /**
     * Build system message. No user input is included; blocks prohibited topics.
     */
    fun buildSystemMessage(): String = """
You are a child-friendly story writer. Respond with valid JSON only, no other text.
Use this exact structure:
{"title": "...", "moral": "...", "story_text": "...", "estimated_duration_seconds": <number, 1-600>}

Rules (mandatory):
- Tone: Always positive, uplifting, and age-appropriate. No scary, sad, or anxious content.
- Moral: Always include a short, clear moral in the "moral" field (e.g. sharing, honesty, kindness).
- Banned topics: No violence, weapons, politics, elections, religious conflict, war, death, drugs, alcohol, self-harm, or adult themes.
- No complex psychological themes. Keep themes simple (friendship, learning, helping, honesty).
- story_text must be suitable for children. estimated_duration_seconds is reading time in seconds (word count / 2.5).
""".trimIndent()

    /**
     * Build main user prompt in conversational style.
     * [theme] and [childName] must be sanitized (from [StorySafetyMiddleware.sanitizeAndValidateInput]).
     * [customPrompt] must be sanitized (from [StorySafetyMiddleware.sanitizeParentCustomPrompt]). Never pass raw user input.
     */
    fun buildUserPrompt(
        age: Int,
        language: String,
        theme: String,
        childName: String,
        maxWordsOverride: Int? = null,
        emotionMode: String? = null,
        customPrompt: String? = null,
        childInterests: String? = null,
        childFavoriteColor: String? = null,
        childFavoriteAnimal: String? = null,
        childTraits: String? = null,
        childAvatarChoice: String? = null
    ): String {
        val safeLang = normalizeLanguage(language)
        val maxWords = maxWordsOverride ?: maxWordsByAge[age.coerceIn(1, 12)] ?: 750
        val vocab = vocabularyHint(age)
        val culture = culturalHint(safeLang)
        val tonePhrase = emotionMode?.let { emotionHint(it) } ?: "Warm, positive, and age-appropriate."

        val opener = buildConversationalOpener(age, childName, theme, safeLang, tonePhrase)
        val extras = buildConversationalExtras(
            childInterests, childFavoriteColor, childFavoriteAnimal, childTraits, childAvatarChoice, customPrompt
        )
        val constraints = "Please keep it to at most $maxWords words. Include a short, clear moral. Respond with only valid JSON: {\"title\": \"...\", \"moral\": \"...\", \"story_text\": \"...\", \"estimated_duration_seconds\": <number>}. No markdown, no code block."

        return listOf(opener, vocab, culture, extras, constraints).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun buildConversationalOpener(age: Int, childName: String, theme: String, lang: String, tonePhrase: String): String {
        return """
I'd like a story for my child $childName. They're $age years old.

We'd love something about $theme—in $lang. $tonePhrase
        """.trimIndent()
    }

    private fun buildConversationalExtras(
        childInterests: String?,
        childFavoriteColor: String?,
        childFavoriteAnimal: String?,
        childTraits: String?,
        childAvatarChoice: String?,
        customPrompt: String?
    ): String {
        val items = mutableListOf<String>()
        childInterests?.takeIf { it.isNotBlank() }?.let { items.add("They enjoy: $it") }
        childFavoriteColor?.takeIf { it.isNotBlank() }?.let { items.add("Favorite color: $it") }
        childFavoriteAnimal?.takeIf { it.isNotBlank() }?.let { items.add("Favorite animal: $it") }
        childTraits?.takeIf { it.isNotBlank() }?.let { items.add("Character traits to reflect: $it") }
        childAvatarChoice?.takeIf { it.isNotBlank() }?.let { items.add("They love this in stories: $it") }
        customPrompt?.takeIf { it.isNotBlank() }?.let { items.add("Specific request: $it") }
        return if (items.isEmpty()) "" else items.joinToString("\n")
    }

    /**
     * Build fallback user prompt (simpler, conversational, same safety). Use when primary prompt fails validation or parse.
     */
    fun buildFallbackUserPrompt(
        age: Int,
        language: String,
        theme: String,
        childName: String,
        maxWordsOverride: Int? = null,
        emotionMode: String? = null,
        customPrompt: String? = null
    ): String {
        val safeLang = normalizeLanguage(language)
        val maxWords = maxWordsOverride ?: maxWordsByAge[age.coerceIn(1, 12)] ?: 750
        val vocab = vocabularyHint(age)
        val tone = emotionMode?.let { emotionHint(it) } ?: "Warm and positive."
        val base = "A short story for $childName (age $age) about $theme, in $safeLang. $tone"
        val custom = customPrompt?.takeIf { it.isNotBlank() }?.let { " Include: $it." } ?: ""
        return "$base.$custom $vocab Maximum $maxWords words. JSON only: title, moral, story_text, estimated_duration_seconds (integer)."
    }

    /** Effective max words for age (capped). */
    fun maxWordsForAge(age: Int): Int = maxWordsByAge[age.coerceIn(1, 12)] ?: 750

    private fun normalizeLanguage(lang: String): String =
        lang.trim().lowercase().take(10).let { if (it in allowedLanguages) it else "en" }

    private fun vocabularyHint(age: Int): String = when {
        age <= 4 -> "Use very simple words and short sentences. No complex ideas."
        age <= 7 -> "Use clear, everyday words. Short to medium sentences."
        else -> "You may use a richer vocabulary; still suitable for children."
    }

    private fun culturalHint(language: String): String = when (language) {
        "ta", "tamil" -> "Set the story in a Tamil-friendly context: family, village or town, respect for elders, friendship, nature. Use culturally appropriate names and values (kindness, sharing, learning)."
        else -> "Keep the story culturally neutral and family-friendly."
    }

    /** Phase 2: Emotion-based tone hints (e.g. bedtime). */
    private fun emotionHint(mode: String): String = when (mode.uppercase()) {
        "CALM", "SOOTHING" -> "Tone: Calm, soothing, gentle, and relaxing. Perfect for bedtime. Avoid excitement or tension. Use soft, peaceful imagery."
        "ADVENTUROUS" -> "Tone: Gently adventurous and uplifting. Mild excitement suitable for daytime. Keep it positive and energizing."
        else -> "Tone: Warm, positive, and age-appropriate."
    }
}
