package com.tamixa.infrastructure.llm

/**
 * Kept for the existing OpenAI → Gemini adapters (translation, narration rewrite).
 *
 * Since Oct 2026 the trigger is the same as [ProviderFallbackPolicy]: any OpenAI failure except a caller
 * input error falls back to Gemini — including 401/403, because Gemini has its own key and billing.
 */
object OpenAiGeminiFallbackPolicies {

    fun isTruthyProperty(raw: String?): Boolean =
        raw?.trim()?.lowercase() in setOf("true", "1", "yes", "on")

    fun shouldTryGeminiAfterOpenAiFailure(error: Throwable): Boolean =
        ProviderFallbackPolicy.shouldFallBack(error)
}
