package com.tamixa.infrastructure.narration

import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.application.port.narration.NarrationLLMResult
import com.tamixa.domain.narration.ToneMode
import com.tamixa.infrastructure.http.ApiErrorExtractor
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * OpenAI adapter for narration formatting.
 * Uses strict prompt template; story content passed in user message only (no interpolation).
 */
@Component
@Conditional(OnNarrationOpenAiPrimaryAdapterCondition::class)
class NarrationOpenAIAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.narration.rewrite-model:gpt-4o}") private val model: String,
    @Value("\${app.narration.max-narration-tokens:16384}") private val maxTokens: Int,
    @Value("\${app.narration.rewrite-temperature:0.7}") private val rewriteTemperature: Double,
    @Value("\${app.story.max-words:900}") private val maxStoryWords: Int
) : NarrationLLMPort {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * System prompt: Tamixa narrator voice. Transforms story text into natural spoken narration
     * that sounds like a real person telling a story—not reading aloud from a book.
     */
    private val systemPrompt = """
        You are Tamixa, a native speaker telling a story aloud to a child in your mother tongue. You sound like a grandparent, teacher, or friend—NOT like someone reading translated text. Use the natural expressions, idioms, and cadence that a native would use when speaking to a child. Avoid phrasing that sounds translated, formal, or textbook-like. Output ONLY the narration—no titles, labels, or meta-commentary. Preserve all story content, characters, plot, and moral. NEVER return input unchanged. Use short segments: 1 to 3 sentences per paragraph. Suited for TTS and voice cloning.
        Continuity: Keep the same order of events and logic as the input. Do not skip scenes, merge unrelated moments, or leave gaps between cause and effect. If two parts of the source feel abrupt, add a brief natural bridge in the spoken style of the language—without inventing new plot.
        Rhythm: Write for spoken delivery. Natural speech has breath points—place sentence endings where a speaker would pause. Avoid long run-on clauses; break at thought boundaries. This helps the voice sound human, not robotic.
        Creativity: Bring the tale to life with fresh, age-appropriate images, light dialogue, and sensory detail where it fits—without changing who does what, outcomes, or the moral. Prefer one vivid moment over generic filler; avoid cliché stacking.
        TTS tags: When the source already includes bracket cues like [Pause 500ms], [Warm tone], or [Calm tone], preserve them unless they break grammar; keep pacing tags where they still fit the rewritten sentence.
    """.trimIndent()

    @Retryable(
        retryFor = [Exception::class],
        noRetryFor = [IllegalStateException::class],
        maxAttempts = 2,
        backoff = Backoff(delay = 1000, multiplier = 1.5)
    )
    override fun formatNarration(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String,
        maxTokens: Int
    ): NarrationLLMResult {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY.")
        }
        val effectiveMax = maxTokens.coerceIn(256, 16384)
        val userPrompt = buildUserPrompt(storyText, age, toneMode, language)
        val request = ChatRequest(
            model = model.trim().ifBlank { "gpt-4o-mini" },
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            maxTokens = effectiveMax,
            temperature = rewriteTemperature.coerceIn(0.0, 0.9)
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
            throw NarrationLLMException("Rewrite failed: $msg")
        } ?: throw NarrationLLMException("Empty response from LLM")
        val content = response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw NarrationLLMException("No content in LLM response")
        val usage = response.usage
        log.debug("Narration LLM response tokens: prompt={}, completion={}",
            usage?.promptTokens, usage?.completionTokens)
        return NarrationLLMResult(
            formattedText = content,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0
        )
    }

    override fun transformWithCustomPrompt(
        storyText: String,
        customPrompt: String,
        maxTokens: Int
    ): NarrationLLMResult {
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY.")
        }
        val trimmedPrompt = customPrompt.trim()
        if (trimmedPrompt.isBlank()) {
            throw IllegalArgumentException("Custom prompt must not be blank.")
        }
        val effectiveMax = maxTokens.coerceIn(256, 16384)
        val systemPrompt = "You are a helpful assistant. Follow the user's instructions exactly. Output only what is requested (e.g. valid JSON when the prompt asks for JSON)—no extra explanations or commentary. When rewriting or restructuring story text, preserve narrative continuity: same event order, no skipped beats or unexplained jumps; use clear transitions; keep vocabulary clear and conversational in the target language as specified in the instructions."
        val userContent = "$trimmedPrompt\n\nStory:\n$storyText"
        val request = ChatRequest(
            model = model.trim().ifBlank { "gpt-4o-mini" },
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userContent)
            ),
            maxTokens = effectiveMax,
            temperature = rewriteTemperature.coerceIn(0.0, 0.9)
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
            throw NarrationLLMException("Custom prompt transform failed: $msg")
        } ?: throw NarrationLLMException("Empty response from LLM")
        val content = response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw NarrationLLMException("No content in LLM response")
        val usage = response.usage
        return NarrationLLMResult(
            formattedText = content,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0
        )
    }

    /**
     * Build user prompt from parameters. Story text is passed as separate block to avoid injection.
     * System prompt defines audiobook storyteller style; user prompt adds audience context and story.
     */
    fun buildUserPrompt(storyText: String, age: Int, toneMode: ToneMode, language: String): String {
        val toneDesc = when (toneMode) {
            ToneMode.CALM -> "soothing, gentle, like a bedtime story"
            ToneMode.EXPRESSIVE -> "warm, animated, full of expression—like sharing an exciting tale"
        }
        val normalizedLanguage = language.trim().lowercase()
        val languageSpecificRules = when (normalizedLanguage) {
            "ta", "tamil" -> "Tamil quality (mandatory): write fluent spoken Tamil in Tamil script. Do not output transliteration artifacts, malformed words, or mixed-language text. Use natural colloquial flow while preserving meaning."
            "hi", "hindi" -> "Hindi quality (mandatory): write fluent spoken Hindi in Devanagari script with natural everyday expressions. Avoid textbook Hindi and avoid script mixing."
            "te", "telugu" -> "Telugu quality (mandatory): write fluent spoken Telugu in Telugu script with natural conversational rhythm. Avoid stiff formal wording and script mixing."
            "kn", "kannada" -> "Kannada quality (mandatory): write fluent spoken Kannada in Kannada script with natural storytelling cadence. Avoid literal translation tone and script mixing."
            "ml", "malayalam" -> "Malayalam quality (mandatory): write fluent spoken Malayalam in Malayalam script with natural oral storytelling flow. Avoid formal bookish phrasing and script mixing."
            "en", "english" -> "English quality (mandatory): write natural spoken English narration with warm conversational flow, not stiff literary prose."
            else -> "Language quality (mandatory): use fully natural spoken style for the target language and keep script consistent for that language."
        }
        val wordCap = maxStoryWords.coerceIn(200, 2500)
        return """
            |Rewrite this story as if you are a NATIVE SPEAKER of this language (code: $language) telling it aloud to a $age-year-old. Tone: $toneDesc.
            |
            |Critical: The listener must hear a native speaker, NOT someone reading translated text. Use expressions, word choices, and rhythm that a native would naturally use. Avoid "translated" or textbook phrasing.
            |
            |Continuity (mandatory): Keep every important event and beat from the input in the same logical order. Do not drop scenes, rush from problem to ending without the middle steps, or leave the listener confused about what happened in between. If needed, add a short connecting sentence in natural $language speech—do not invent new plot or characters.
            |
            |$languageSpecificRules
            |
            |Natural Speech Rules (critical—follow these):
            |- For English: use contractions and everyday words where natural ("it's", "don't", "that's"). For other languages: use the equivalent colloquial, spoken forms natives use with children—not stiff written or translated-sounding phrasing.
            |- Vary how you start sentences: "And then...", "So you see...", "Well, one day...", "Can you imagine?" instead of repeating "The" or formal structures.
            |- Use natural storytelling connectors: "So...", "Now...", "Oh! And then...", "You see..." as a real speaker would.
            |- Mix sentence lengths: short punchy phrases alongside longer flowing ones. Real speech isn't uniform.
            |- Replace formal or literary phrases with how people actually talk. "He proceeded to" → "So he went ahead and". "She exclaimed" → "She said, excited".
            |- Imagine the child is right there. Include subtle engagement: "And guess what happened?", "Can you believe it?" where it fits.
            |- Add light emotional color: a soft "Oh!" for surprise, warmth for kind moments, gentle urgency for suspense—but keep it natural, not overacted.
            |- Avoid: robotic lists, stiff transitions, formal vocabulary, anything that sounds like text being read aloud.
            |- Rhythm: End sentences at natural breath points. Avoid long chains of clauses. A speaker pauses between thoughts—mirror that in sentence structure.
            |
            |What NOT to do:
            |- Do not sound like you're reading from a book or script.
            |- Do not use overly formal or written-style language.
            |- Do not make every sentence the same structure or length.
            |
            |CRITICAL—Inline markers:
            |- Preserve existing narration markers and keep marker text in English.
            |- Use only this vocabulary when adding markers: [Warm tone], [Gentle tone], [Calm tone], [Happy tone], [Excited tone], [Playful tone], [Curious tone], [Wonder tone], [Reassuring tone], [Thoughtful tone], [Soft voice], [Whispered tone], [Emotional tone], [Soft emotional tone], [Celebration tone], [Storyteller tone], [Slow pacing], [Medium pacing], [Brisk pacing], [Scene opens softly], [Scene shifts], [A gentle moment], [A magical moment], [A quiet pause], [A joyful moment], [A surprise moment], [Closing tone], [Pause 300ms], [Pause 500ms], [Pause 700ms], [Pause 1s], [Pause 1.5s].
            |- Never translate marker labels. Never output malformed marker text.
            |
            |Length (strict): The narration must be at most $wordCap words (count words in the output language). If the input is longer, compress faithfully—keep every beat of the plot and the moral—while staying under $wordCap words. If the input is shorter, you may add tasteful detail and dialogue as long as you stay within $wordCap words and do not invent new plot points or characters.
            |
            |Output only the narration. No titles, labels, or meta-commentary. Preserve all story content, characters, plot, and moral.
            |Formatting hygiene: never output the literal token "nn". Use proper line breaks between paragraphs.
            |
            |Story:
            |$storyText
        """.trimMargin()
    }

    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @JsonProperty("max_tokens") val maxTokens: Int,
        @JsonProperty("temperature") val temperature: Double
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

class NarrationLLMException(message: String) : RuntimeException(message)
