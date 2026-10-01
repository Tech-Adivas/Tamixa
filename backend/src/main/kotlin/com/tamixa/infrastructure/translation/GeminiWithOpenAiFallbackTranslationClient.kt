package com.tamixa.infrastructure.translation

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.gemini.GeminiApiClient
import com.tamixa.infrastructure.llm.OnTranslationGeminiWithOpenAiFallbackCondition
import com.tamixa.infrastructure.llm.ProviderFallbackRunner
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Tries [GeminiTranslationClient] first; when Gemini fails uses [OpenAITranslationClient].
 * Enable with `TRANSLATION_PROVIDER=gemini` and `TRANSLATION_GEMINI_FALLBACK_TO_OPENAI=true` (requires `OPENAI_API_KEY`).
 */
@Component
@Conditional(OnTranslationGeminiWithOpenAiFallbackCondition::class)
class GeminiWithOpenAiFallbackTranslationClient(
    restTemplate: RestTemplate,
    objectMapper: ObjectMapper,
    meterRegistry: MeterRegistry,
    geminiApiClient: GeminiApiClient,
    appProperties: AppProperties,
    @Value("\${app.openai.api-key:}") openaiApiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") openaiBaseUrl: String,
    @Value("\${app.openai.model:gpt-4o-mini}") openaiModel: String,
    @Value("\${app.translation-pipeline.translation-timeout-ms:30000}") defaultTimeoutMs: Long,
    @Value("\${app.story.max-words:900}") maxStoryWords: Int,
) : TranslationClientPort by FallbackTranslationClient(
    primary = GeminiTranslationClient(geminiApiClient, objectMapper, meterRegistry, appProperties, maxStoryWords),
    fallback = OpenAITranslationClient(
        restTemplate, objectMapper, meterRegistry, openaiApiKey, openaiBaseUrl, openaiModel,
        defaultTimeoutMs, maxStoryWords,
    ),
    runner = ProviderFallbackRunner("translation", "gemini", "openai", meterRegistry),
)
