package com.tamixa.infrastructure.translation

import com.tamixa.infrastructure.llm.ProviderFallbackSettings
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/** Real OpenAI translation client: `TRANSLATION_PROVIDER=openai` without Gemini fallback. */
class OnTranslationOpenAiOnlyCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        return ProviderFallbackSettings.translationProvider(env) == "openai" &&
            !ProviderFallbackSettings.translationOpenAiThenGemini(env)
    }
}

/** OpenAI first, then Gemini when OpenAI fails (`TRANSLATION_OPENAI_FALLBACK_TO_GEMINI=true`). */
class OnTranslationOpenAiWithGeminiFallbackCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        ProviderFallbackSettings.translationOpenAiThenGemini(context.environment)
}
