package com.tamixa.infrastructure.narration

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.gemini.GeminiApiClient
import com.tamixa.infrastructure.llm.OnNarrationGeminiOpenAiFallbackAdapterCondition
import com.tamixa.infrastructure.llm.ProviderFallbackRunner
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Pipeline rewrite + Regenerate: Gemini first, OpenAI when Gemini fails (billing, quota, retired model, outage).
 * Enable with `AI_LLM_PROVIDER=gemini` and `AI_LLM_FALLBACK_PROVIDER=openai` (requires `OPENAI_API_KEY`).
 */
@Component
@Conditional(OnNarrationGeminiOpenAiFallbackAdapterCondition::class)
class GeminiWithOpenAiFallbackNarrationLlmAdapter(
    restTemplate: RestTemplate,
    objectMapper: ObjectMapper,
    geminiApiClient: GeminiApiClient,
    appProperties: AppProperties,
    meterRegistry: MeterRegistry,
    @Value("\${app.openai.api-key:}") openaiApiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") openaiBaseUrl: String,
    @Value("\${app.narration.rewrite-model:gpt-4o}") rewriteModel: String,
    @Value("\${app.narration.max-narration-tokens:16384}") maxNarrationTokens: Int,
    @Value("\${app.narration.rewrite-temperature:0.7}") rewriteTemperature: Double,
    @Value("\${app.story.max-words:900}") maxStoryWords: Int,
) : NarrationLLMPort by FallbackNarrationLlm(
    primary = NarrationGeminiAdapter(
        geminiApiClient, appProperties, maxNarrationTokens, rewriteTemperature, maxStoryWords,
    ),
    fallback = NarrationOpenAIAdapter(
        restTemplate, objectMapper, openaiApiKey, openaiBaseUrl, rewriteModel,
        maxNarrationTokens, rewriteTemperature, maxStoryWords,
    ),
    runner = ProviderFallbackRunner("narration", "gemini", "openai", meterRegistry),
)
