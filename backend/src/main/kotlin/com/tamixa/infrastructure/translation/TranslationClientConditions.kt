package com.tamixa.infrastructure.translation

import com.tamixa.infrastructure.llm.OpenAiGeminiFallbackPolicies
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/** Real OpenAI translation client: `TRANSLATION_PROVIDER=openai` without Gemini fallback. */
class OnTranslationOpenAiOnlyCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val p = context.environment.getProperty("app.translation.provider", "simulated")?.trim()?.lowercase() ?: "simulated"
        if (p != "openai") return false
        val fb = context.environment.getProperty("app.translation.openai-fallback-to-gemini", "false")
        return !OpenAiGeminiFallbackPolicies.isTruthyProperty(fb)
    }
}

/** OpenAI first, then Gemini when OpenAI fails with quota/rate-limit style errors. */
class OnTranslationOpenAiWithGeminiFallbackCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val p = context.environment.getProperty("app.translation.provider", "simulated")?.trim()?.lowercase() ?: "simulated"
        if (p != "openai") return false
        val fb = context.environment.getProperty("app.translation.openai-fallback-to-gemini", "false")
        return OpenAiGeminiFallbackPolicies.isTruthyProperty(fb)
    }
}
