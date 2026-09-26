package com.tamixa.infrastructure.translation

import com.tamixa.application.port.TranslatedContent
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.gemini.GeminiApiClient
import com.tamixa.infrastructure.llm.OpenAiGeminiFallbackPolicies
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Tries [OpenAITranslationClient] first; on OpenAI quota / rate-limit style failures, uses [GeminiTranslationClient].
 * Enable with `TRANSLATION_PROVIDER=openai` and `TRANSLATION_OPENAI_FALLBACK_TO_GEMINI=true` (requires `GEMINI_API_KEY`).
 */
@Component
@Conditional(OnTranslationOpenAiWithGeminiFallbackCondition::class)
class OpenAiWithGeminiFallbackTranslationClient(
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
) : TranslationClientPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val openAi = OpenAITranslationClient(
        restTemplate,
        objectMapper,
        meterRegistry,
        openaiApiKey,
        openaiBaseUrl,
        openaiModel,
        defaultTimeoutMs,
        maxStoryWords,
    )
    private val gemini = GeminiTranslationClient(
        geminiApiClient,
        objectMapper,
        meterRegistry,
        appProperties,
        maxStoryWords,
    )

    override fun translate(sourceLang: String, targetLang: String, text: String, timeoutMs: Long): String =
        try {
            openAi.translate(sourceLang, targetLang, text, timeoutMs)
        } catch (e: Exception) {
            if (OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(e)) {
                log.warn(
                    "OpenAI translation failed ({}), falling back to Gemini {}->{}",
                    e.message,
                    sourceLang,
                    targetLang,
                )
                gemini.translate(sourceLang, targetLang, text, timeoutMs)
            } else {
                throw e
            }
        }

    override fun translateStructured(
        sourceLang: String,
        targetLang: String,
        title: String?,
        content: String,
        moral: String?,
        timeoutMs: Long,
        parentContentNote: String?,
        parentDiscussionPrompts: List<String>?,
        speakAlongPrompt: String?,
    ): TranslatedContent =
        try {
            openAi.translateStructured(
                sourceLang, targetLang, title, content, moral, timeoutMs,
                parentContentNote, parentDiscussionPrompts, speakAlongPrompt,
            )
        } catch (e: Exception) {
            if (OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(e)) {
                log.warn(
                    "OpenAI translateStructured failed ({}), falling back to Gemini {}->{}",
                    e.message,
                    sourceLang,
                    targetLang,
                )
                gemini.translateStructured(
                    sourceLang, targetLang, title, content, moral, timeoutMs,
                    parentContentNote, parentDiscussionPrompts, speakAlongPrompt,
                )
            } else {
                throw e
            }
        }
}
