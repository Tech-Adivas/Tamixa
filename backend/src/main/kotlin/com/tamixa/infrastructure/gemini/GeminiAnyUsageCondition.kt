package com.tamixa.infrastructure.gemini

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * True when Gemini HTTP is needed: primary LLM is gemini, translation uses gemini, and/or cover stills use gemini.
 */
class GeminiAnyUsageCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        val llm = env.getProperty("app.llm.provider", "openai")?.trim()?.lowercase() ?: "openai"
        val translation =
            env.getProperty("app.translation.provider", "simulated")?.trim()?.lowercase() ?: "simulated"
        val translationOpenAiGeminiFallback = env.getProperty("app.translation.openai-fallback-to-gemini", "false")
            ?.trim()?.lowercase() in setOf("true", "1", "yes", "on")
        val narrationRewriteGeminiFallback = env.getProperty("app.narration.openai-rewrite-fallback-to-gemini", "false")
            ?.trim()?.lowercase() in setOf("true", "1", "yes", "on")
        val coverImage =
            env.getProperty("app.image-generation.provider", "openai")?.trim()?.lowercase() ?: "openai"
        return llm == "gemini" ||
            translation == "gemini" ||
            (translation == "openai" && translationOpenAiGeminiFallback) ||
            (llm == "openai" && narrationRewriteGeminiFallback) ||
            coverImage == "gemini"
    }
}
