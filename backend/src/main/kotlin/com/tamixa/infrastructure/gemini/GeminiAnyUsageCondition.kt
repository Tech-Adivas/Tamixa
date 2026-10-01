package com.tamixa.infrastructure.gemini

import com.tamixa.infrastructure.llm.ProviderFallbackSettings
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * True when Gemini HTTP is needed: as primary LLM, as backup LLM (`AI_LLM_FALLBACK_PROVIDER=gemini`),
 * for translation (primary or backup), for narration rewrite backup, and/or for cover stills.
 */
class GeminiAnyUsageCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        val llm = ProviderFallbackSettings.llmProvider(env)
        val coverImage =
            env.getProperty("app.image-generation.provider", "openai")?.trim()?.lowercase() ?: "openai"
        return llm == "gemini" ||
            ProviderFallbackSettings.llmFallbackProvider(env) == "gemini" ||
            ProviderFallbackSettings.translationProvider(env) == "gemini" ||
            ProviderFallbackSettings.translationOpenAiThenGemini(env) ||
            ProviderFallbackSettings.narrationOpenAiThenGemini(env) ||
            coverImage == "gemini"
    }
}
