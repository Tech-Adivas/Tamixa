package com.tamixa.infrastructure.narration

import com.tamixa.infrastructure.llm.ProviderFallbackSettings
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/** Primary OpenAI narration adapter when LLM is OpenAI and no Gemini backup is configured. */
class OnNarrationOpenAiPrimaryAdapterCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        return ProviderFallbackSettings.llmProvider(env) == "openai" &&
            !ProviderFallbackSettings.narrationOpenAiThenGemini(env)
    }
}

/**
 * OpenAI rewrite with Gemini backup: `AI_LLM_PROVIDER=openai` and either
 * `NARRATION_OPENAI_REWRITE_FALLBACK_TO_GEMINI=true` or `AI_LLM_FALLBACK_PROVIDER=gemini`.
 */
class OnNarrationOpenAiGeminiFallbackAdapterCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        ProviderFallbackSettings.narrationOpenAiThenGemini(context.environment)
}
