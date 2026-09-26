package com.tamixa.infrastructure.llm

import org.springframework.web.client.HttpClientErrorException

/**
 * Detects OpenAI failures where trying Google Gemini may succeed (e.g. exhausted OpenAI quota).
 * Does not treat 401/403 as fallback-worthy (bad key — Gemini would not help).
 */
object OpenAiGeminiFallbackPolicies {

    fun isTruthyProperty(raw: String?): Boolean =
        raw?.trim()?.lowercase() in setOf("true", "1", "yes", "on")

    fun shouldTryGeminiAfterOpenAiFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            when (current) {
                is HttpClientErrorException -> {
                    val code = current.statusCode.value()
                    if (code == 429) return true
                    if (code == 401 || code == 403) return false
                }
            }
            val msg = (current.message ?: "").lowercase()
            if (msg.contains("insufficient_quota")) return true
            if (msg.contains("rate_limit")) return true
            if (msg.contains("rate limit") && msg.contains("429")) return true
            if (msg.contains("too many requests")) return true
            current = current.cause
        }
        return false
    }
}
