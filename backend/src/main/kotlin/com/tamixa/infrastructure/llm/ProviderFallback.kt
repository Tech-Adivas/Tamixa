package com.tamixa.infrastructure.llm

import org.springframework.core.env.Environment

/**
 * When to switch to the backup AI provider.
 *
 * Any provider failure (quota, billing disabled, bad/expired key, 5xx, timeout, empty answer) is worth
 * retrying on the *other* provider, because it has its own account, key and billing.
 * The only exception is a caller input error ([IllegalArgumentException]) — the backup would reject it too.
 */
object ProviderFallbackPolicy {

    fun shouldFallBack(error: Throwable): Boolean {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 10) {
            if (current is IllegalArgumentException) return false
            current = current.cause
            depth++
        }
        return true
    }

    /** One-line, key-free reason for logs. */
    fun reason(error: Throwable): String =
        (error.message ?: error.javaClass.simpleName)
            .replace(Regex("""([?&]key=)[^&"\s]+"""), "$1***")
            .replace(Regex("""(sk-[A-Za-z0-9_-]{4})[A-Za-z0-9_-]+"""), "$1***")
            .replace(Regex("\\s+"), " ")
            .take(240)
}

/**
 * Reads the fallback switches from the Spring environment (normalised, case-insensitive).
 *
 * | Env var                                     | Effect                                                         |
 * |---------------------------------------------|----------------------------------------------------------------|
 * | `AI_LLM_FALLBACK_PROVIDER=openai\|gemini\|none` | Story generation, Regenerate, moderation and narration rewrite |
 * | `TRANSLATION_OPENAI_FALLBACK_TO_GEMINI=true`| Translation: OpenAI first, then Gemini                         |
 * | `TRANSLATION_GEMINI_FALLBACK_TO_OPENAI=true`| Translation: Gemini first, then OpenAI                         |
 * | `NARRATION_TTS_FALLBACK=openai\|none`         | Narration audio: Google TTS first, then OpenAI TTS             |
 */
object ProviderFallbackSettings {

    private val truthy = setOf("true", "1", "yes", "on")

    private fun Environment.norm(key: String, default: String): String =
        (getProperty(key) ?: default).trim().lowercase()

    fun llmProvider(env: Environment): String = env.norm("app.llm.provider", "openai")

    /** Backup LLM provider, or `none` when unset / same as primary / unknown. */
    fun llmFallbackProvider(env: Environment): String {
        val fb = env.norm("app.llm.fallback-provider", "none")
        if (fb !in setOf("openai", "gemini")) return "none"
        return if (fb == llmProvider(env)) "none" else fb
    }

    /** OpenAI primary with Gemini backup for narration rewrite (legacy flag or the new LLM fallback switch). */
    fun narrationOpenAiThenGemini(env: Environment): Boolean =
        llmProvider(env) == "openai" && (
            env.norm("app.narration.openai-rewrite-fallback-to-gemini", "false") in truthy ||
                llmFallbackProvider(env) == "gemini"
            )

    /** Gemini primary with OpenAI backup for narration rewrite. */
    fun narrationGeminiThenOpenAi(env: Environment): Boolean =
        llmProvider(env) == "gemini" && llmFallbackProvider(env) == "openai"

    fun translationProvider(env: Environment): String = env.norm("app.translation.provider", "simulated")

    fun translationOpenAiThenGemini(env: Environment): Boolean =
        translationProvider(env) == "openai" &&
            env.norm("app.translation.openai-fallback-to-gemini", "false") in truthy

    fun translationGeminiThenOpenAi(env: Environment): Boolean =
        translationProvider(env) == "gemini" &&
            env.norm("app.translation.gemini-fallback-to-openai", "false") in truthy

    fun ttsProvider(env: Environment): String = env.norm("app.narration.tts-provider", "tamixa")

    fun ttsFallback(env: Environment): String {
        val fb = env.norm("app.narration.tts-fallback", "none")
        return if (fb == "openai" && ttsProvider(env) == "google") "openai" else "none"
    }
}
