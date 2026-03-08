package com.araro.infrastructure.narration

import com.araro.application.port.narration.NarrationLLMPort
import com.araro.application.port.narration.NarrationLLMResult
import com.araro.domain.narration.ToneMode
import com.araro.infrastructure.http.ApiErrorExtractor
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * OpenAI adapter for narration formatting.
 * Uses strict prompt template; story content passed in user message only (no interpolation).
 */
@Component
class NarrationOpenAIAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.narration.rewrite-model:gpt-4o}") private val model: String,
    @Value("\${app.narration.max-narration-tokens:1024}") private val maxTokens: Int,
    @Value("\${app.narration.rewrite-temperature:0.6}") private val rewriteTemperature: Double
) : NarrationLLMPort {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * System prompt: output rules only. Full audiobook storyteller style is in user prompt.
     * Punctuation affects TTS intonation (commas, questions, exclamations).
     */
    private val systemPrompt = """
        You transform story text into spoken narration. Output ONLY the narration—no titles, labels, or meta-commentary.
        Preserve all story content, characters, plot, and moral.
        NEVER return input unchanged. Every paragraph must be transformed for spoken delivery.
        Keep paragraphs short (2-4 sentences).
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
        val effectiveMax = maxTokens.coerceIn(256, 2048)
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

    /**
     * Build user prompt from parameters. Story text is passed as separate block to avoid injection.
     * System prompt defines audiobook storyteller style; user prompt adds audience context and story.
     */
    fun buildUserPrompt(storyText: String, age: Int, toneMode: ToneMode, language: String): String {
        val toneDesc = when (toneMode) {
            ToneMode.CALM -> "soothing, gentle, bedtime voice"
            ToneMode.EXPRESSIVE -> "warm, engaging, animated voice"
        }
        return """
            |You are a professional audiobook storyteller.
            |
            |Narrate the story in a natural conversational style that native speakers of the language would use in everyday speech.
            |
            |Storytelling Style:
            |Warm, engaging, expressive, and human-like.
            |
            |Delivery Guidelines:
            |- Speak like a storyteller narrating to children or a general audience.
            |- Use conversational language instead of formal written language.
            |- Keep sentences flowing naturally like spoken conversation.
            |- Add emotional tone during suspense, surprise, and humorous moments.
            |- Use brief, natural pauses between sentences—avoid long gaps.
            |- Slow down slightly during suspense or emotional moments.
            |- Add slight excitement during funny or surprising parts.
            |
            |Voice Personality:
            |Friendly storyteller, similar to a teacher, grandparent, or podcast narrator.
            |
            |Audience:
            |Children or family audiences aged 2–12. For this story: $age-year-old. Use a $toneDesc. Language: $language
            |
            |Narration Behavior:
            |- Avoid robotic reading.
            |- Sound natural and expressive.
            |- Emphasize key moments in the story.
            |- Maintain smooth storytelling rhythm—minimize pauses between lines.
            |
            |Speech Characteristics:
            |Pacing: medium-slow
            |Emotion: warm, playful, engaging
            |Clarity: clear and expressive
            |Accent: natural native accent of the language used in the story.
            |
            |Goal:
            |The listener should feel like they are listening to a real human storyteller rather than an AI reading text.
            |
            |Output only the narration. No titles, labels, or meta-commentary. Preserve all story content, characters, plot, and moral.
            |
            |Now narrate the following story using this storytelling style.
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
