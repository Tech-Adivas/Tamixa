package com.tamixa.api.dev

import com.tamixa.api.ApiVersion
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.gemini.GeminiUrlBuilder
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Dev-only: proves every configured LLM path really generates text (not just "host reachable").
 *
 * GET /api/v1/dev/verify-llms
 *  - providers.openai  : tiny chat completion with OPENAI_MODEL
 *  - providers.gemini  : tiny generateContent with GEMINI_MODEL (same model the Gemini adapters use)
 *  - paths.storyLlm    : the active [OpenAIPort] bean (story generation / moderation / summaries)
 *  - paths.translation : the active [TranslationClientPort] bean (en → ta)
 *  - paths.narration   : the active [NarrationLLMPort] bean (pipeline rewrite)
 *
 * Each call is capped at a few output tokens (cost ≈ zero). API keys are redacted from every message.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/dev")
@Profile("dev")
class DevLlmVerifyController(
    private val restTemplate: RestTemplate,
    private val appProperties: AppProperties,
    private val storyLlmProvider: ObjectProvider<OpenAIPort>,
    private val translationProvider: ObjectProvider<TranslationClientPort>,
    private val narrationProvider: ObjectProvider<NarrationLLMPort>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/verify-llms")
    fun verifyLlms(): ResponseEntity<Map<String, Any>> {
        val providers = linkedMapOf<String, Any>(
            "openai" to checkOpenAi(),
            "gemini" to checkGemini(appProperties.llm.gemini.model),
        )
        val paths = linkedMapOf<String, Any>(
            "storyLlm" to runPath(storyLlmProvider.ifAvailable) { p ->
                p.completeChat("You are a test probe.", "Reply with exactly: OK", 8)
            },
            "translation" to runPath(translationProvider.ifAvailable) { p ->
                p.translate("en", "ta", "Good night, little star.", 60_000)
            },
            "narration" to runPath(narrationProvider.ifAvailable) { p ->
                p.transformWithCustomPrompt("The cat sat on the mat.", "Repeat the text exactly.", 40).formattedText
            },
        )
        val all = providers.values + paths.values
        val failed = all.count { (it as? Map<*, *>)?.get("status") == "ERROR" }
        val body = linkedMapOf(
            "llmProvider" to appProperties.llm.provider,
            "translationProvider" to appProperties.translation.provider,
            "geminiUrlStyle" to appProperties.llm.gemini.apiUrlStyle,
            "geminiBaseUrl" to appProperties.llm.gemini.baseUrl,
            "providers" to providers,
            "paths" to paths,
            "overall" to if (failed == 0) "OK" else "DEGRADED ($failed failing)",
        )
        return if (failed == 0) ResponseEntity.ok(body) else ResponseEntity.status(503).body(body)
    }

    private fun checkOpenAi(): Map<String, Any?> {
        val key = appProperties.openai.apiKey
        if (key.isBlank()) return mapOf("status" to "SKIPPED", "message" to "OPENAI_API_KEY not set")
        val model = appProperties.openai.model
        return timed(model) {
            val headers = HttpHeaders().apply {
                setBearerAuth(key)
                contentType = MediaType.APPLICATION_JSON
            }
            val req = mapOf(
                "model" to model,
                "messages" to listOf(mapOf("role" to "user", "content" to "Reply with exactly: OK")),
                "max_tokens" to 5,
            )
            val res = restTemplate.exchange(
                "${appProperties.openai.baseUrl.trimEnd('/')}/v1/chat/completions",
                HttpMethod.POST, HttpEntity(req, headers), Map::class.java,
            ).body
            @Suppress("UNCHECKED_CAST")
            val choice = (res?.get("choices") as? List<Map<String, Any?>>)?.firstOrNull()
            @Suppress("UNCHECKED_CAST")
            ((choice?.get("message") as? Map<String, Any?>)?.get("content") as? String).orEmpty()
        }
    }

    private fun checkGemini(model: String): Map<String, Any?> {
        val g = appProperties.llm.gemini
        if (g.apiKey.isBlank()) return mapOf("status" to "SKIPPED", "message" to "GEMINI_API_KEY not set", "model" to model)
        return timed(model) {
            val url = GeminiUrlBuilder.generateContentUrl(
                g.baseUrl, model, URLEncoder.encode(g.apiKey, StandardCharsets.UTF_8), g.apiUrlStyle,
            )
            val req = mapOf(
                "contents" to listOf(mapOf("role" to "user", "parts" to listOf(mapOf("text" to "Reply with exactly: OK")))),
                "generationConfig" to mapOf("maxOutputTokens" to 16, "temperature" to 0),
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val res = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(req, headers), Map::class.java).body
            @Suppress("UNCHECKED_CAST")
            val cand = (res?.get("candidates") as? List<Map<String, Any?>>)?.firstOrNull()
            @Suppress("UNCHECKED_CAST")
            val parts = (cand?.get("content") as? Map<String, Any?>)?.get("parts") as? List<Map<String, Any?>>
            parts?.mapNotNull { it["text"] as? String }?.joinToString("").orEmpty()
                .ifBlank { "(empty text; finishReason=${cand?.get("finishReason")})" }
        }
    }

    private fun <T : Any> runPath(bean: T?, call: (T) -> String): Map<String, Any?> {
        if (bean == null) return mapOf("status" to "SKIPPED", "message" to "no bean configured")
        val impl = AopUtils.getTargetClass(bean).simpleName
        return timed(impl) { call(bean) }
    }

    private fun timed(label: String, block: () -> String): Map<String, Any?> {
        val t0 = System.currentTimeMillis()
        return try {
            val out = block()
            mapOf("status" to "OK", "using" to label, "latencyMs" to System.currentTimeMillis() - t0, "sample" to out.trim().take(80))
        } catch (e: Exception) {
            val msg = redact(e.message ?: e.javaClass.simpleName)
            log.warn("LLM verify failed using={} message={}", label, msg)
            mapOf("status" to "ERROR", "using" to label, "latencyMs" to System.currentTimeMillis() - t0, "message" to msg, "hint" to hintFor(msg))
        }
    }

    companion object {
        private val KEY_PARAM = Regex("""([?&]key=)[^&"\s<]+""")
        private val BEARER = Regex("""(sk-[A-Za-z0-9_-]{4})[A-Za-z0-9_-]+""")

        fun redact(s: String): String =
            s.replace(KEY_PARAM, "$1***").replace(BEARER, "$1***").replace(Regex("\\s+"), " ").take(600)

        fun hintFor(msg: String): String? = when {
            msg.contains("billing", ignoreCase = true) ->
                "Google Cloud project has billing disabled. Enable billing, or use a Google AI Studio key with GEMINI_BASE_URL=https://generativelanguage.googleapis.com and GEMINI_API_URL_STYLE=google-ai"
            msg.contains("401") || msg.contains("invalid_api_key", ignoreCase = true) || msg.contains("API key not valid", ignoreCase = true) ->
                "API key rejected — create a new key and update .env"
            msg.contains("429") || msg.contains("quota", ignoreCase = true) || msg.contains("insufficient_quota", ignoreCase = true) ->
                "Quota/credit exhausted — add credit or raise the quota"
            msg.contains("404") -> "Model or endpoint not found — check the model name and GEMINI_API_URL_STYLE / base URL pair"
            msg.contains("403") -> "Key lacks permission for this API/model"
            else -> null
        }
    }
}
