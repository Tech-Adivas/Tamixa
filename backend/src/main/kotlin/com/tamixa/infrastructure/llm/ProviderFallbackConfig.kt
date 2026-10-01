package com.tamixa.infrastructure.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.narration.TtsClientPort
import com.tamixa.infrastructure.gemini.GeminiLlmClient
import com.tamixa.infrastructure.narration.FallbackTtsClient
import com.tamixa.infrastructure.narration.GoogleCloudTtsClientAdapter
import com.tamixa.infrastructure.narration.OpenAITtsClientAdapter
import com.tamixa.infrastructure.observability.AiApiMetrics
import com.tamixa.infrastructure.openai.OpenAIClient
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.web.client.RestTemplate

/**
 * Wires the backup-provider decorators. Each bean is `@Primary`, so every service that injects the port
 * (StoryService, StoryModerationService, NeuralVoiceStrategy, …) transparently gets the fallback version.
 */
@Configuration
class ProviderFallbackConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    /** `AI_LLM_FALLBACK_PROVIDER=openai|gemini` — story generation, summaries, moderation. */
    @Bean
    @Primary
    @Conditional(OnStoryLlmFallbackCondition::class)
    fun storyLlmWithFallback(
        env: Environment,
        openAiClient: ObjectProvider<OpenAIClient>,
        geminiLlmClient: ObjectProvider<GeminiLlmClient>,
        meterRegistry: ObjectProvider<MeterRegistry>,
    ): OpenAIPort {
        val primaryName = ProviderFallbackSettings.llmProvider(env)
        val fallbackName = ProviderFallbackSettings.llmFallbackProvider(env)
        fun clientFor(name: String): OpenAIPort = when (name) {
            "openai" -> openAiClient.getObject()
            "gemini" -> geminiLlmClient.getObject()
            else -> throw IllegalStateException("Unknown LLM provider '$name' (use openai or gemini)")
        }
        log.info("Story LLM: primary={} backup={}", primaryName, fallbackName)
        return FallbackStoryLlm(
            primary = clientFor(primaryName),
            fallback = clientFor(fallbackName),
            runner = ProviderFallbackRunner("story-llm", primaryName, fallbackName, meterRegistry.ifAvailable),
        )
    }

    /** `NARRATION_TTS_PROVIDER=google` + `NARRATION_TTS_FALLBACK=openai` — narration audio. */
    @Bean
    @Primary
    @Conditional(OnGoogleTtsWithOpenAiFallbackCondition::class)
    fun ttsWithFallback(
        googleTts: GoogleCloudTtsClientAdapter,
        restTemplate: RestTemplate,
        objectMapper: ObjectMapper,
        aiApiMetrics: ObjectProvider<AiApiMetrics>,
        meterRegistry: ObjectProvider<MeterRegistry>,
        @Value("\${app.openai.api-key:}") openaiApiKey: String,
        @Value("\${app.openai.base-url:https://api.openai.com}") openaiBaseUrl: String,
        @Value("\${app.narration.openai-tts-model:tts-1-hd}") openaiTtsModel: String,
        @Value("\${app.narration.openai-tts-speed:1.0}") openaiTtsSpeed: Double,
    ): TtsClientPort {
        if (openaiApiKey.isBlank()) {
            log.warn("NARRATION_TTS_FALLBACK=openai but OPENAI_API_KEY is blank — backup TTS will be skipped")
        }
        log.info("Narration TTS: primary=google backup=openai ({})", openaiTtsModel)
        return FallbackTtsClient(
            primary = googleTts,
            fallback = OpenAITtsClientAdapter(
                restTemplate, objectMapper, aiApiMetrics.ifAvailable,
                openaiApiKey, openaiBaseUrl, openaiTtsModel, openaiTtsSpeed,
            ),
            runner = ProviderFallbackRunner("tts", "google", "openai", meterRegistry.ifAvailable),
        )
    }
}
