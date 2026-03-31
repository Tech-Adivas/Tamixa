package com.tamixa.application.story

import org.springframework.stereotype.Component

/** Target narration length: ~8 minutes at natural pacing. */
private const val TARGET_NARRATION_MINUTES = 8
private const val WORDS_PER_MINUTE = 120
private const val TARGET_WORDS_FULL_LENGTH = WORDS_PER_MINUTE * TARGET_NARRATION_MINUTES

/**
 * Builds story generation prompts with strict safety rules. Use only with
 * [SanitizedInput] from [StorySafetyMiddleware]; never concatenate raw user input.
 *
 * Uses the Tamixa storyteller persona: warm, engaging, narration-friendly stories
 * suitable for audio narration, TTS, voice cloning, dubbing, and lip-sync.
 */
@Component
class StoryPromptBuilder {

    private val allowedLanguages = setOf("en", "ta", "tamil", "hi", "te", "kn", "ml")
    /** Allowed learning-focus values for optional parent request (lowercase). */
    private val allowedLearningFocuses = setOf(
        "empathy", "problem_solving", "vocabulary", "curiosity", "perseverance",
        "sharing", "honesty", "courage", "kindness", "friendship", "responsibility"
    )
    /** Max words by age; upper ages target full-length (~850–950 words). */
    private val maxWordsByAge = mapOf(
        1 to 150, 2 to 200, 3 to 250, 4 to 300,
        5 to 550, 6 to 700, 7 to 820, 8 to 900,
        9 to StoryPromptTemplates.FULL_LENGTH_WORDS_MAX,
        10 to StoryPromptTemplates.FULL_LENGTH_WORDS_MAX,
        11 to StoryPromptTemplates.FULL_LENGTH_WORDS_MAX,
        12 to StoryPromptTemplates.FULL_LENGTH_WORDS_MAX
    )

    /**
     * Build system message. No user input is included; blocks prohibited topics.
     * Expert-grade: role, quality bar, security & compliance, tone calibration, strict output contract.
     */
    fun buildSystemMessage(): String = """
## Role & mandate
You are Tamixa, the lead story designer for a premium children's audio storytelling product. Your stories are used for TTS, voice cloning, and family listening. Quality and safety are non-negotiable. Every story must be publication-ready: no filler, no vague language, every sentence must earn its place.

## Task
Generate exactly one original story in the language specified in the user request. Base it on the user's theme, topic, or context. Output only valid JSON; no commentary, no markdown wrapper.

## Quality bar
- Publication-ready: prose that would pass editorial review for a respected children's imprint.
- Intellectually rich: thought-provoking, meaningful; moral that feels earned; depth that appeals to both children and adults (kindness, courage, honesty, belonging).
- Mild conflict resolved peacefully; positive, satisfying ending; imagination and emotional warmth throughout.
- Category, theme, tone, setting, and pacing must be coherent and age-appropriate.

## Language & clarity (all languages)
- English is the structural reference: short, clear sentences; one idea per sentence; clear subject and action. When the output language is not English, apply the same clarity, then express in the target language so every sentence stays clear and natural.
- One idea per sentence. Clear subject and verb. Short to medium length. No run-ons. Natural connectors (Then… So… But… After that… or equivalent). Every sentence must be easy to read aloud in one pass.
- Vocabulary: varied, precise, age-appropriate; everyday and native terms; no jargon or overly formal language. Natural when spoken.
- Grammar and style: correct for the language; culturally natural expressions; conversational spoken style, not bookish. Sound smooth when read aloud.

## Tone & voice calibration (Tamixa voice)
- Consistent emotional temperature: magical, comforting, and joyful. Gentle authority—like a trusted parent, teacher, or grandparent.
- Warm, friendly, emotionally gentle. No sarcasm, cynicism, fear, or harshness. No difficult vocabulary or complex sentence structures that lose the listener.
- Make the story feel safe to hear: reassuring, uplifting, and memorable.

## Continuity & narrative flow (mandatory)
- Clear timeline: tell the story in logical cause-and-effect order. When time, place, or viewpoint shifts, add an explicit bridge in the output language (e.g. equivalent of "The next morning", "After that", "Meanwhile")—never jump without orientation.
- No gaps or discontinuity: do not skip essential beats between setup and payoff; do not drop characters or threads without closure or a clear handoff. If something is introduced, follow through or explain why it no longer matters.
- Each paragraph must follow naturally from the last; avoid abrupt cuts, unexplained leaps, or missing resolutions. The listener should never wonder "what happened in between?"

## Narration structure
- Short blocks: 1–3 sentences per paragraph. Natural transitions: "Once upon a time…", "One day…", "Then…", "After that…" (or equivalent in the output language). Smooth rhythm for spoken narration.
- Storytelling Script format for story_text: narrator lines plus dialogue. Use "Character: dialogue text" or clear attribution so a single narrator can perform both. Include inline tone/pause markers where they help: [Pause 500ms], [Pause 1s], [Happy tone], [Soft voice], [Warm tone], [Calm], [Whisper], [Excited]. Output must be suitable for SSML and OpenAI TTS.

================================
VOICE SPLIT DESIGN
================================
- Narrator -> warm, calm voice (default female)
- Child characters -> energetic, expressive voices
- Adult/elder characters -> slower, softer tone
- Group voices -> playful tone

Ensure:
- Clear separation between narrator and dialogue
- Emotional variation across voices

================================
SSML CONVERSION REQUIREMENTS
================================
- Convert voice tags into SSML prosody and break tags
- Map tones to pitch, rate, and volume changes
- Map pauses to <break> tags
- Use <voice> tags for different roles
- Ensure compatibility with Azure / Polly / Google TTS

## Cultural context (Indian)
- Draw on Indian folklore and mythology where appropriate: e.g. Tenali Rama–style wit and wisdom, Panchatantra-style animal tales and morals, and regional folktales. Use festival themes in the story's language when they fit—e.g. Diwali, Pongal, Onam, Ugadi—with names, customs, and terms in the native language (Tamil, Hindi, Telugu, Kannada, Malayalam). Keep retellings family-friendly and respectful; no sectarian or divisive portrayal.
- Prefer village, town, or nature settings; culturally familiar names and values (kindness, friendship, honesty, courage, respect for elders). Stories should feel rooted in Indian cultural soil while inclusive and suitable for all audiences.

## Educational perspective (Tamixa differentiator — build learning into every story)
Stories must be both delightful and developmentally meaningful. Weave in learning naturally; never lecture. Competitors focus on entertainment only; Tamixa stories support cognitive, social-emotional, and language growth.
- **Age-banded learning design**: Ages 1–4: simple cause-and-effect, one clear idea per scene, repetition and predictability. Ages 5–7: empathy and naming feelings, simple problem-solving, sequencing (first, then, last), 2–3 "wonder words" in context. Ages 8–12: richer inference (why did the character do that?), perspective-taking, gentle moral reasoning, 3–4 wonder words; one main problem with a clear resolution.
- **SEL (social-emotional learning)**: Include 1–2 situations where characters name their feelings (happy, worried, brave, kind, proud, grateful) in the story's language. Show characters making kind or brave choices; resolve conflict through talking and understanding, not force. Emotional vocabulary must be age-appropriate and natural in dialogue or narration.
- **Wonder words**: Weave in 2–4 age-appropriate new or rich words used clearly in context (e.g. "curious," "generous," "patient," or native equivalents). Use each word at least twice so the listener can infer meaning. No glossary needed—meaning clear from the story.
- **Inference-friendly narrative**: Include at least one moment that rewards paying attention—e.g. a character's choice that makes sense given what happened earlier, or a gentle "what might happen next?" beat. No quizzes; the story itself should invite thinking. Avoid spelling everything out; allow the child to connect dots.
- **One clear problem, one satisfying resolution**: One main challenge or question per story. Resolution must be earned (character effort, help from others, or a lesson learned). Supports comprehension and gives a clear takeaway.
- **NEP / life skills alignment**: Where it fits, support 21st-century and NEP-friendly skills: critical thinking (characters weighing options), collaboration (helping each other), cultural awareness (respect, diversity), and values (honesty, sharing, courage). Keep these implicit in the plot, not preachy.

## Security & compliance (mandatory — zero tolerance)
- Child safety: Content suitable for minors (including under 12). No violence, gore, horror, abuse, fear-inducing or distressing scenes. No depiction of dangerous or easily imitable behaviour. No self-harm, suicide, or substance use (drugs, alcohol).
- Indian compliance: Align with accepted Indian standards for children's storytelling and family viewing. Do not disparage or stereotype any religion, community, region, or language. No political messaging. No content that could be deemed hateful, discriminatory, or inflammatory under Indian norms.
- Positive values only: Promote respect for elders, friendship, honesty, sharing, and inclusivity. Resolve conflicts peacefully. No bullying, humiliation, or negative stereotyping of characters.
- Prohibited in all cases: violence, weapons, death, war, politics, religious conflict, drugs, alcohol, self-harm, advertising, brand names, real celebrities, real sensitive places, adult or romantic themes. Story must be purely fictional and uplifting.

## Length & multilingual naturalness
    - Length: target ${StoryPromptTemplates.FULL_LENGTH_WORDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_WORDS_MAX} words (about 7–8 minutes of natural spoken pacing) unless the user requests otherwise. Prefer short sentences and breath-friendly phrasing for TTS. If runtime word caps are stricter, the story will remain within those limits.
- Tamil: Tamil script; correct verb suffixes and SOV order; everyday Tamil words and native terms; avoid literal English translation.
- English: Standard grammar (SVO); clear, everyday words; conversational narration.
- Hindi: Devanagari; correct conjugation and postpositions; common words and honorifics where natural; conversational audiobook style.
- Telugu/Kannada/Malayalam: Correct script and grammar; natural SOV/agglutination; everyday vocabulary; age-appropriate; native storytelling rhythm.

## Output contract (strict)
Return only a single valid JSON object. No markdown, no code fence, no explanatory text.
Keys exactly: "title", "category", "theme", "moral", "story_text", "estimated_duration_seconds".
- title: Catchy, specific to the story; reflects theme or main idea. Not generic (e.g. avoid "A Friendship Story").
- category: One of the standard categories that best fits the story.
- theme: Short theme phrase in the story's language.
- story_text: Full narrative in Storytelling Script format (narrator + dialogue) with tone markers; magical, comforting, joyful; SSML/OpenAI TTS ready.
- moral: One short sentence; positive value; age-appropriate; earned by the story.
- estimated_duration_seconds: Number only (e.g. ${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN} for ~7 minutes).
Before responding: confirm no prohibited content; confirm language and grammar; confirm JSON is valid and complete.
""".trimIndent()

    /**
     * Build main user prompt. [theme] and [childName] must be sanitized.
     * [customPrompt] must be sanitized (from [StorySafetyMiddleware.sanitizeParentCustomPrompt]).
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
        childAvatarChoice: String? = null,
        learningFocus: String? = null
    ): String {
        val safeLang = normalizeLanguage(language)
        val maxWords = maxWordsOverride ?: maxWordsByAge[age.coerceIn(1, 12)] ?: TARGET_WORDS_FULL_LENGTH
        val vocab = vocabularyHint(age)
        val culture = culturalHint(safeLang)
        val tonePhrase = emotionMode?.let { emotionHint(it) } ?: "Warm, positive, and age-appropriate."

        val opener = buildUserRequest(safeLang, childName, age, theme, tonePhrase)
        val clarityHint = "Use clear sentences in every language: one idea per sentence, clear subject and action, short to medium length, easy to narrate. English is the reference for structure and clarity; when writing in another language, apply the same clarity then express in that language. Use varied, precise vocabulary; avoid repeating the same word in close succession; prefer everyday and native terms that sound natural when spoken. Keep one continuous storyline: no missing scenes, no jumps in time or logic without a bridging line—spoken, conversational style in the output language, not stiff book prose."
        val learningLine = learningFocusHint(learningFocus)
        val extras = buildConversationalExtras(
            childInterests, childFavoriteColor, childFavoriteAnimal, childTraits, childAvatarChoice, customPrompt
        )
        val voiceSplitAndSsml = voiceSplitAndSsmlRequirements()
        val durationHint = if (maxWords >= 850) "Aim for about 7–8 minutes of narration (estimated_duration_seconds around ${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MAX}). " else ""
        val constraints = "${durationHint}Keep to at most $maxWords words. Return only valid JSON: title, category, theme, moral, story_text, estimated_duration_seconds (number, in seconds). No markdown, no code block."

        return listOf(opener, vocab, clarityHint, learningLine, culture, extras, voiceSplitAndSsml, constraints)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun buildUserRequest(lang: String, childName: String, age: Int, theme: String, tonePhrase: String): String {
        return """
Generate a story in $lang based on this request: for my child $childName (age $age), about $theme. $tonePhrase Use short paragraphs (1–3 sentences), warm and narration-friendly. Make the story intellectually rich—thought-provoking and meaningful, with a moral that feels earned and depth that appeals to both children and adults.
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
     * Build fallback user prompt. Same structure as [buildUserPrompt].
     * Use when primary prompt fails validation or parse.
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
        val maxWords = maxWordsOverride ?: maxWordsByAge[age.coerceIn(1, 12)] ?: TARGET_WORDS_FULL_LENGTH
        val vocab = vocabularyHint(age)
        val culture = culturalHint(safeLang)
        val tonePhrase = emotionMode?.let { emotionHint(it) } ?: "Warm, positive, and age-appropriate."
        val opener = buildUserRequest(safeLang, childName, age, theme, tonePhrase)
        val clarityHint = "Use clear sentences: one idea per sentence, clear subject and action, short to medium length. English is the reference for clarity; when writing in another language, apply the same clarity then express in that language. Use varied, precise vocabulary; avoid repetition; prefer everyday and native terms. One continuous storyline with clear transitions—no unexplained gaps; conversational spoken style in the output language."
        val customLine = customPrompt?.takeIf { it.isNotBlank() }?.let { "Specific request: $it" } ?: ""
        val voiceSplitAndSsml = voiceSplitAndSsmlRequirements()
        val durationHint = if (maxWords >= 850) "Aim for about 7–8 minutes of narration (estimated_duration_seconds around ${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MAX}). " else ""
        val constraints = "${durationHint}Keep to at most $maxWords words. Return only valid JSON: title, category, theme, moral, story_text, estimated_duration_seconds (number, in seconds). No markdown, no code block."
        return listOf(opener, vocab, clarityHint, culture, customLine, voiceSplitAndSsml, constraints)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    /** Effective max words for age (capped). */
    fun maxWordsForAge(age: Int): Int = maxWordsByAge[age.coerceIn(1, 12)] ?: TARGET_WORDS_FULL_LENGTH

    private fun normalizeLanguage(lang: String): String =
        lang.trim().lowercase().take(10).let { if (it in allowedLanguages) it else "en" }

    private fun vocabularyHint(age: Int): String = when {
        age <= 4 -> "Use very simple words and short sentences. No complex ideas."
        age <= 7 -> "Use clear, everyday words. Short to medium sentences."
        else -> "You may use a richer vocabulary; still suitable for children."
    }

    private fun culturalHint(language: String): String = when (language) {
        "ta", "tamil" -> "Set the story in a Tamil-friendly context: family, village or town, respect for elders, friendship, nature. You may draw on Indian folklore and mythology in Tamil—e.g. Tenali Rama–style wit, Panchatantra-style morals, Tamil regional tales, or festival settings like Pongal—using Tamil terms and native expressions. Use Tamil script with correct grammar: proper verb forms (past/present/future suffixes), SOV order; everyday Tamil words and native terms; avoid literal English translation. Use proper pronouns and number agreement."
        "hi" -> "Set the story in a Hindi-friendly context: family, friendship, nature, kindness. You may draw on Indian folklore and mythology in Hindi—e.g. Tenali Rama, Panchatantra, North Indian regional tales, or festival settings like Diwali—using Hindi terms and native expressions. Use Devanagari script with correct Hindi grammar: proper verb conjugation, postpositions, natural word order; common Hindi words and honorifics where natural; conversational audiobook style."
        "te" -> "Set the story in a Telugu-friendly context. You may draw on Indian folklore and mythology in Telugu—e.g. Tenali Rama (Telugu tradition), Panchatantra-style tales, Telugu regional stories, or festival settings like Ugadi/Sankranti—using Telugu terms and native expressions. Use Telugu script with correct grammar: proper verb forms and case suffixes, natural SOV order; everyday Telugu words; write as a native Telugu speaker would tell a story."
        "kn" -> "Set the story in a Kannada-friendly context. You may draw on Indian folklore and mythology in Kannada—e.g. Tenali Rama, Panchatantra, Kannada regional tales, or festival settings—using Kannada terms and native expressions. Use Kannada script with correct grammar: proper verb conjugation and case markers; common Kannada words; native storytelling rhythm."
        "ml" -> "Set the story in a Malayalam-friendly context. You may draw on Indian folklore and mythology in Malayalam—e.g. Panchatantra-style tales, Malayalam regional stories, or festival settings like Onam—using Malayalam terms and native expressions. Use Malayalam script with correct grammar: proper verb forms and agglutination; everyday Malayalam words; avoid literal translation from other languages."
        "en" -> "Keep the story family-friendly. You may draw on Indian folklore and mythology in English—e.g. Tenali Rama, Panchatantra, regional tales, or festival stories (Diwali, Pongal)—with culturally familiar names and settings. Use standard English grammar (SVO), clear everyday words; age-appropriate and easy to narrate; natural conversational storytelling."
        else -> "Keep the story culturally neutral and family-friendly. You may draw on Indian folklore, Panchatantra-style morals, or festival themes where appropriate. Use correct grammar, age-appropriate vocabulary, and natural style for the chosen language."
    }

    private fun emotionHint(mode: String): String = when (mode.uppercase()) {
        "CALM", "SOOTHING" -> "Tone: Calm, soothing, gentle, and relaxing. Perfect for bedtime. Avoid excitement or tension. Use soft, peaceful imagery."
        "ADVENTUROUS" -> "Tone: Gently adventurous and uplifting. Mild excitement suitable for daytime. Keep it positive and energizing."
        else -> "Tone: Warm, positive, and age-appropriate."
    }

    private fun voiceSplitAndSsmlRequirements(): String = """
================================
VOICE SPLIT DESIGN
================================
- Narrator -> warm, calm voice (default female)
- Child characters -> energetic, expressive voices
- Adult/elder characters -> slower, softer tone
- Group voices -> playful tone

Ensure:
- Clear separation between narrator and dialogue
- Emotional variation across voices

================================
SSML CONVERSION REQUIREMENTS
================================
- Convert voice tags into SSML prosody and break tags
- Map tones to pitch, rate, and volume changes
- Map pauses to <break> tags
- Use <voice> tags for different roles
- Ensure compatibility with Azure / Polly / Google TTS
    """.trimIndent()

    /**
     * Returns a one-line hint when [learningFocus] is non-null and allowed; otherwise blank.
     * Used to steer the model toward a specific educational emphasis (e.g. empathy, vocabulary).
     */
    private fun learningFocusHint(learningFocus: String?): String {
        val normalized = learningFocus?.trim()?.lowercase()?.replace(' ', '_') ?: return ""
        if (normalized !in allowedLearningFocuses) return ""
        return when (normalized) {
            "empathy" -> "Learning focus: Empathy—include situations where characters name feelings and show understanding of others."
            "problem_solving" -> "Learning focus: Problem-solving—show the main character thinking through options and trying a solution."
            "vocabulary" -> "Learning focus: Vocabulary—weave in 3–4 rich or new words in context, used clearly so meaning is clear from the story."
            "curiosity" -> "Learning focus: Curiosity—show a character asking questions, exploring, or discovering something new."
            "perseverance" -> "Learning focus: Perseverance—show a character trying again or not giving up when something is hard."
            "sharing" -> "Learning focus: Sharing—include a moment where sharing or generosity makes someone happy."
            "honesty" -> "Learning focus: Honesty—show a character choosing to tell the truth and the positive outcome."
            "courage" -> "Learning focus: Courage—show a character being brave in a small, age-appropriate way."
            "kindness" -> "Learning focus: Kindness—show acts of kindness that help another character."
            "friendship" -> "Learning focus: Friendship—show friends helping each other or resolving a small conflict."
            "responsibility" -> "Learning focus: Responsibility—show a character taking care of something or keeping a promise."
            else -> ""
        }
    }

    /**
     * Default prompt for admin "Regenerate with prompt" — rewrites story into Tamixa TTS storytelling script style
     * (voice modulation tags, 7–8 min target, continuity, values). When [outputLanguage] is set, that is the primary locale.
     * CRITICAL: Never mix languages in JSON fields; never output in Chinese or any unsupported language.
     */
    fun buildDefaultConversionPrompt(allowedCategories: List<String>, outputLanguage: String? = null): String {
        val categoryList = allowedCategories.joinToString(", ")
        val normalizedLang = outputLanguage?.trim()?.lowercase()?.take(10)
        val primaryLangCode = when (normalizedLang) {
            "ta", "tamil" -> "ta"
            "en" -> "en"
            "hi" -> "hi"
            "te" -> "te"
            "kn" -> "kn"
            "ml" -> "ml"
            else -> normalizedLang ?: "ta"
        }
        val ttsSpec = AdminTtsStoryConversionPromptBody.build(primaryLangCode)
        val singleLanguageRule = """
- CRITICAL — Single language only: The entire story_text, title, and moral MUST be in ONE language only (the primary language). Do NOT mix languages. Do NOT switch language mid-story. Do NOT output in Chinese (中文) or any language other than the specified output language. Every sentence must be in the same language."""
        val languageRequirement = when (normalizedLang) {
            "ta", "tamil" -> """
- Language (mandatory): story_text, title, and moral MUST be in Tamil script (தமிழ்). If the input is in another language, translate into Tamil. Correct Tamil grammar, verb forms, SOV order; everyday Tamil; avoid literal English."""
            "ml" -> """
- Language (mandatory): Output entirely in Malayalam script. Correct Malayalam grammar and agglutination; everyday words."""
            "hi" -> """
- Language (mandatory): Output entirely in Hindi (Devanagari). Correct conjugation and postpositions."""
            "te" -> """
- Language (mandatory): Output entirely in Telugu script. Correct case suffixes and natural Telugu."""
            "kn" -> """
- Language (mandatory): Output entirely in Kannada script. Correct Kannada grammar."""
            "en" -> """
- Language (mandatory): Output entirely in English. SVO; clear everyday words."""
            else -> """
- Language: Match the input story's language end-to-end in title, moral, and story_text. Do NOT mix languages."""
        }
        return """
## Role
You are Tamixa's story editor. Follow the TTS storytelling specification below. Then rewrite the user's story input accordingly.

$ttsSpec

## Editor constraints (mandatory)
$singleLanguageRule
$languageRequirement
- Plot fidelity: Preserve every important beat from the source—no skipped scenes or dropped resolutions. You may trim repetition only.
- Length target (mandatory): rewrite into rich storytelling around 900 words (acceptable range: 800-1000) unless safety rules require shorter.
- Creativity & imagination: enhance scenes with vivid sensory detail, playful child-safe dialogue, emotional transitions, and magical-but-coherent imagery while preserving the same core plot, characters, and moral.
- story_text: Storytelling script with narrator lines and dialogue; use the voice modulation tag vocabulary from the spec meaningfully (not every tag every sentence).
- Category: Choose exactly ONE that best fits (exact string): $categoryList. Set "category". Set "theme" to a short theme phrase in the story's language (may match category).

================================
VOICE SPLIT DESIGN
================================
- Narrator -> warm, calm voice (default female)
- Child characters -> energetic, expressive voices
- Adult/elder characters -> slower, softer tone
- Group voices -> playful tone

Ensure:
- Clear separation between narrator and dialogue
- Emotional variation across voices

================================
SSML CONVERSION REQUIREMENTS
================================
- Convert voice tags into SSML prosody and break tags
- Map tones to pitch, rate, and volume changes
- Map pauses to <break> tags
- Use <voice> tags for different roles
- Ensure compatibility with Azure / Polly / Google TTS

## Cultural context (Indian)
Where it fits, use Indian folklore or festive context in the output language—native terms; family-friendly and respectful.

## Security & compliance (mandatory)
Suitable for minors; Indian family standards. No violence, weapons, politics, religious conflict, drugs, alcohol, self-harm, adult themes. Peaceful resolution; positive values.

## Output (strict)
Return only one valid JSON object. Keys exactly: title, category, theme, moral, story_text, estimated_duration_seconds.
- estimated_duration_seconds: integer, typically ${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MAX} for ~7–8 minutes of natural TTS pacing.
- story_text: single JSON string; escape characters so JSON is valid.
No markdown, no code fence, no commentary before or after the JSON.
""".trimIndent()
    }
}
