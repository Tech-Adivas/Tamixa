package com.araro.infrastructure.translation

import com.araro.application.port.TranslationClientPort
import com.araro.application.port.TranslatedContent
import com.araro.infrastructure.http.ApiErrorExtractor
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
 */
@Component
@ConditionalOnProperty(name = ["app.translation.provider"], havingValue = "openai")
class OpenAITranslationClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.openai.model:gpt-4o-mini}") private val model: String,
    @Value("\${app.translation-pipeline.translation-timeout-ms:30000}") private val defaultTimeoutMs: Long
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
        val result = translateStructured(sourceLang, targetLang, null, text, null, timeoutMs)
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
        timeoutMs: Long
    ): TranslatedContent {
        log.info("OpenAI translation start {}->{} contentLen={}", sourceLang, targetLang, content.length)
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY for translation.")
        }
        val srcName = langNames[sourceLang] ?: sourceLang
        val tgtName = langNames[targetLang] ?: targetLang
        val systemPrompt = """
            You are a translator for children's stories. Translate from $srcName to $tgtName.
            Output valid JSON only with keys: title (or null), content, moral (or null).
            Preserve the story's meaning, tone, and cultural context. Use age-appropriate vocabulary.
            Do NOT use bullets (•), asterisks (*), em-dashes (—), or other special symbols at the start of lines or paragraphs. Output plain text suitable for text-to-speech.
        """.trimIndent()
        val parts = buildList {
            title?.let { add("TITLE: $it") }
            add("CONTENT:\n$content")
            moral?.let { add("MORAL: $it") }
        }
        val userPrompt = "Translate the following story. Return JSON: {\"title\": \"...\" or null, \"content\": \"...\", \"moral\": \"...\" or null}\n\n${parts.joinToString("\n\n")}"
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            maxTokens = 2048,
            temperature = 0.0
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
        val rawContent = response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalStateException("No content in translation response")
        val result = parseTranslationResponse(rawContent, title, content, moral, sourceLang, targetLang)
        log.info("OpenAI translation success {}->{} resultLen={}", sourceLang, targetLang, result.content.length)
        return result
    }

    private fun parseTranslationResponse(
        raw: String,
        originalTitle: String?,
        originalContent: String,
        originalMoral: String?,
        sourceLang: String,
        targetLang: String
    ): TranslatedContent {
        return try {
            val json = raw.removeSurrounding("```json").removeSurrounding("```").trim()
            val node = objectMapper.readTree(json)
            val rawContent = node["content"]?.asText()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Missing or empty content in translation response")
            TranslatedContent(
                title = sanitizeTranslationText(node["title"]?.takeIf { !it.isNull }?.asText()?.takeIf { it.isNotBlank() } ?: originalTitle),
                content = sanitizeTranslationText(rawContent) ?: rawContent,
                moral = sanitizeTranslationText(node["moral"]?.takeIf { !it.isNull }?.asText()?.takeIf { it.isNotBlank() } ?: originalMoral)
            )
        } catch (e: Exception) {
            log.warn("Translation JSON parse failed, extracting from raw: {}", e.message)
            val extracted = extractContentFromRaw(raw).ifBlank { raw }
            TranslatedContent(
                title = sanitizeTranslationText(originalTitle),
                content = sanitizeTranslationText(extracted) ?: extracted,
                moral = sanitizeTranslationText(originalMoral)
            )
        }
    }

    /** Removes labels (content:, title:, moral:), "[lang]" prefixes, and stray quotes/commas from translation output. */
    private fun sanitizeTranslationText(text: String?): String? {
        if (text.isNullOrBlank()) return text
        var t = text.trim()
        t = Regex("^\\s*\\[[a-z]{2,5}]\\s*", RegexOption.IGNORE_CASE).replace(t, "")
        t = Regex("^\\s*\"?(content|title|moral)\"?:?\\s*[\"']?", RegexOption.IGNORE_CASE).replace(t, "")
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
