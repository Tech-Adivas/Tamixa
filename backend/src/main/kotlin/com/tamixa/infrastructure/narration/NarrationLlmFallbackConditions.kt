package com.tamixa.infrastructure.narration

import com.tamixa.infrastructure.llm.OpenAiGeminiFallbackPolicies
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/** Primary OpenAI narration adapter when LLM is OpenAI and rewrite Gemini fallback is off. */
class OnNarrationOpenAiPrimaryAdapterCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val llm = context.environment.getProperty("app.llm.provider", "openai")?.trim()?.lowercase() ?: "openai"
        if (llm != "openai") return false
        val fb = context.environment.getProperty("app.narration.openai-rewrite-fallback-to-gemini", "false")
        return !OpenAiGeminiFallbackPolicies.isTruthyProperty(fb)
    }
}

/** OpenAI rewrite with Gemini fallback when LLM stays OpenAI but OpenAI quota/rate limits break rewrite. */
class OnNarrationOpenAiGeminiFallbackAdapterCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val llm = context.environment.getProperty("app.llm.provider", "openai")?.trim()?.lowercase() ?: "openai"
        if (llm != "openai") return false
        val fb = context.environment.getProperty("app.narration.openai-rewrite-fallback-to-gemini", "false")
        return OpenAiGeminiFallbackPolicies.isTruthyProperty(fb)
    }
}
