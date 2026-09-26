package com.tamixa.infrastructure.narration

import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.application.port.narration.NarrationLLMResult
import com.tamixa.domain.narration.ToneMode
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.gemini.GeminiApiClient
import com.tamixa.infrastructure.llm.OpenAiGeminiFallbackPolicies
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Pipeline rewrite: try OpenAI first; on quota / rate-limit style failures use Gemini.
 * Enable with `AI_LLM_PROVIDER=openai` and `NARRATION_OPENAI_REWRITE_FALLBACK_TO_GEMINI=true` (requires `GEMINI_API_KEY`).
 */
@Component
@Conditional(OnNarrationOpenAiGeminiFallbackAdapterCondition::class)
class OpenAiWithGeminiFallbackNarrationLlmAdapter(
    restTemplate: RestTemplate,
    objectMapper: ObjectMapper,
    geminiApiClient: GeminiApiClient,
    appProperties: AppProperties,
    @Value("\${app.openai.api-key:}") openaiApiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") openaiBaseUrl: String,
    @Value("\${app.narration.rewrite-model:gpt-4o}") rewriteModel: String,
    @Value("\${app.narration.max-narration-tokens:16384}") maxNarrationTokens: Int,
    @Value("\${app.narration.rewrite-temperature:0.7}") rewriteTemperature: Double,
    @Value("\${app.story.max-words:900}") maxStoryWords: Int,
) : NarrationLLMPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val openAi = NarrationOpenAIAdapter(
        restTemplate,
        objectMapper,
        openaiApiKey,
        openaiBaseUrl,
        rewriteModel,
        maxNarrationTokens,
        rewriteTemperature,
        maxStoryWords,
    )
    private val gemini = NarrationGeminiAdapter(
        geminiApiClient,
        appProperties,
        maxNarrationTokens,
        rewriteTemperature,
        maxStoryWords,
    )

    override fun formatNarration(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String,
        maxTokens: Int,
    ): NarrationLLMResult =
        try {
            openAi.formatNarration(storyText, age, toneMode, language, maxTokens)
        } catch (e: Exception) {
            if (OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(e)) {
                log.warn("OpenAI narration rewrite failed ({}), falling back to Gemini lang={}", e.message, language)
                gemini.formatNarration(storyText, age, toneMode, language, maxTokens)
            } else {
                throw e
            }
        }

    override fun transformWithCustomPrompt(
        storyText: String,
        customPrompt: String,
        maxTokens: Int,
    ): NarrationLLMResult =
        try {
            openAi.transformWithCustomPrompt(storyText, customPrompt, maxTokens)
        } catch (e: Exception) {
            if (OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(e)) {
                log.warn("OpenAI custom prompt transform failed ({}), falling back to Gemini", e.message)
                gemini.transformWithCustomPrompt(storyText, customPrompt, maxTokens)
            } else {
                throw e
            }
        }
}
