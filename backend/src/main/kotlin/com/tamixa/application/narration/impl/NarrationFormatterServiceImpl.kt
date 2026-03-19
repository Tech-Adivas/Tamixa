package com.tamixa.application.narration.impl

import com.tamixa.application.narration.NarrationFormatResult
import com.tamixa.application.narration.NarrationFormatterService
import com.tamixa.application.narration.RewriteConcurrencyLimiter
import com.tamixa.application.narration.RewriteResult
import com.tamixa.application.narration.RewriteService
import com.tamixa.application.narration.TokenUsageService
import com.tamixa.application.narration.toRewriteResult
import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.domain.narration.ToneMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Formats story text into conversational narration via LLM.
 * Enforces token limit, logs usage, retries once on failure.
 */
@Service
class NarrationFormatterServiceImpl(
    private val llm: NarrationLLMPort,
    private val tokenUsageService: TokenUsageService,
    private val rewriteConcurrencyLimiter: RewriteConcurrencyLimiter,
    @Value("\${app.narration.max-narration-tokens:16384}") private val maxTokens: Int
) : NarrationFormatterService, RewriteService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun rewrite(storyText: String, age: Int, toneMode: ToneMode, language: String): RewriteResult =
        formatNarration(storyText, age, toneMode, language).toRewriteResult()

    override fun formatNarration(
        storyText: String,
        age: Int,
        toneMode: ToneMode,
        language: String
    ): NarrationFormatResult {
        log.info("Rewrite start lang={} storyLen={} (OpenAI)", language, storyText.length)
        if (tokenUsageService.isLimitExceeded()) {
            log.warn("Daily token limit exceeded lang={}—returning original text (flat read). Set NARRATION_DAILY_TOKEN_LIMIT higher.", language)
            return NarrationFormatResult(scriptText = storyText, promptTokens = 0, completionTokens = 0)
        }
        val effectiveMax = maxTokens.coerceIn(256, 16384)
        return try {
            val result = rewriteConcurrencyLimiter.withPermit {
                llm.formatNarration(storyText, age, toneMode, language, effectiveMax)
            }
            val total = result.promptTokens + result.completionTokens
            if (!tokenUsageService.recordAndCheckLimit(total)) {
                log.warn("Token limit exceeded after formatting. Usage recorded.")
            }
            log.info("Rewrite done lang={} promptTokens={} completionTokens={}", language, result.promptTokens, result.completionTokens)
            NarrationFormatResult(
                scriptText = trimLeadingSpecialSymbols(result.formattedText),
                promptTokens = result.promptTokens,
                completionTokens = result.completionTokens
            )
        } catch (e: Exception) {
            log.warn("Narration formatting failed lang={}, retrying once: {}", language, e.message)
            try {
                val result = rewriteConcurrencyLimiter.withPermit {
                    llm.formatNarration(storyText, age, toneMode, language, effectiveMax)
                }
                val total = result.promptTokens + result.completionTokens
                tokenUsageService.recordAndCheckLimit(total)
                NarrationFormatResult(
                    trimLeadingSpecialSymbols(result.formattedText),
                    result.promptTokens,
                    result.completionTokens
                )
            } catch (retryEx: Exception) {
                log.error("Narration formatting retry failed lang={}—returning original text (flat read). Check OPENAI_API_KEY, rate limits (429). Cause: {}", language, retryEx.message, retryEx)
                NarrationFormatResult(
                    scriptText = trimLeadingSpecialSymbols(storyText),
                    promptTokens = 0,
                    completionTokens = 0
                )
            }
        }
    }

    /** Trims leading special symbols so TTS does not read them aloud at audio start. */
    private fun trimLeadingSpecialSymbols(text: String): String {
        if (text.isBlank()) return text
        return text.split("\n").joinToString("\n") { para ->
            para.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trimStart()
        }.trim()
    }
}
