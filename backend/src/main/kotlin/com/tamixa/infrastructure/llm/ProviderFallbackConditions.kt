package com.tamixa.infrastructure.llm

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

// ---------------------------------------------------------------------------------------------
// Story LLM ([com.tamixa.application.port.OpenAIPort]): story generation, Regenerate, moderation
// ---------------------------------------------------------------------------------------------

/** OpenAI story client is needed as primary (`AI_LLM_PROVIDER=openai`) or as backup (`AI_LLM_FALLBACK_PROVIDER=openai`). */
class OnOpenAiStoryLlmCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        return ProviderFallbackSettings.llmProvider(env) == "openai" ||
            ProviderFallbackSettings.llmFallbackProvider(env) == "openai"
    }
}

/** Gemini story client is needed as primary or as backup. */
class OnGeminiStoryLlmCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        return ProviderFallbackSettings.llmProvider(env) == "gemini" ||
            ProviderFallbackSettings.llmFallbackProvider(env) == "gemini"
    }
}

/** A valid backup LLM provider is configured (different from the primary). */
class OnStoryLlmFallbackCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        ProviderFallbackSettings.llmFallbackProvider(context.environment) != "none"
}

// ---------------------------------------------------------------------------------------------
// Narration rewrite ([com.tamixa.application.port.narration.NarrationLLMPort])
// ---------------------------------------------------------------------------------------------

class OnNarrationGeminiPrimaryAdapterCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        return ProviderFallbackSettings.llmProvider(env) == "gemini" &&
            !ProviderFallbackSettings.narrationGeminiThenOpenAi(env)
    }
}

class OnNarrationGeminiOpenAiFallbackAdapterCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        ProviderFallbackSettings.narrationGeminiThenOpenAi(context.environment)
}

// ---------------------------------------------------------------------------------------------
// Translation ([com.tamixa.application.port.TranslationClientPort])
// ---------------------------------------------------------------------------------------------

class OnTranslationGeminiOnlyCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        return ProviderFallbackSettings.translationProvider(env) == "gemini" &&
            !ProviderFallbackSettings.translationGeminiThenOpenAi(env)
    }
}

class OnTranslationGeminiWithOpenAiFallbackCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        ProviderFallbackSettings.translationGeminiThenOpenAi(context.environment)
}

// ---------------------------------------------------------------------------------------------
// Narration audio ([com.tamixa.application.port.narration.TtsClientPort])
// ---------------------------------------------------------------------------------------------

/** `NARRATION_TTS_PROVIDER=google` + `NARRATION_TTS_FALLBACK=openai`. */
class OnGoogleTtsWithOpenAiFallbackCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        ProviderFallbackSettings.ttsFallback(context.environment) == "openai"
}
