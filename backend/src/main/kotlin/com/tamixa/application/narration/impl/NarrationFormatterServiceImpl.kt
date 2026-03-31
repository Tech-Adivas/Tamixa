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
    private val pauseMarkerRegex = Regex("""\[\s*Pause\s*\d+(?:ms|s)\s*]""", RegexOption.IGNORE_CASE)
    private val toneMarkerRegex = Regex(
        """\[\s*(?:Warm\s*tone|Gentle\s*tone|Calm(?:\s*tone)?|Happy\s*tone|Excited\s*tone|Playful\s*tone|Curious\s*tone|Wonder\s*tone|Reassuring\s*tone|Thoughtful\s*tone|Soft\s*voice|Whisper(?:ed)?\s*tone|Emotional\s*tone|Soft\s*emotional\s*tone|Celebration\s*tone|Storyteller\s*tone|Closing\s*tone)\s*]""",
        RegexOption.IGNORE_CASE
    )

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
            val firstPassText = sanitizeForNarration(trimLeadingSpecialSymbols(result.formattedText))
            val repaired = runQualityRepairIfNeeded(storyText, firstPassText, language, effectiveMax)
            val finalText = repaired?.first ?: firstPassText
            val repairTokens = repaired?.second ?: 0
            val total = result.promptTokens + result.completionTokens + repairTokens
            if (!tokenUsageService.recordAndCheckLimit(total)) {
                log.warn("Token limit exceeded after formatting. Usage recorded.")
            }
            log.info("Rewrite done lang={} promptTokens={} completionTokens={}", language, result.promptTokens, result.completionTokens)
            NarrationFormatResult(
                scriptText = finalText,
                promptTokens = result.promptTokens,
                completionTokens = result.completionTokens + repairTokens
            )
        } catch (e: Exception) {
            log.warn("Narration formatting failed lang={}, retrying once: {}", language, e.message)
            try {
                val result = rewriteConcurrencyLimiter.withPermit {
                    llm.formatNarration(storyText, age, toneMode, language, effectiveMax)
                }
                val firstPassText = sanitizeForNarration(trimLeadingSpecialSymbols(result.formattedText))
                val repaired = runQualityRepairIfNeeded(storyText, firstPassText, language, effectiveMax)
                val finalText = repaired?.first ?: firstPassText
                val repairTokens = repaired?.second ?: 0
                val total = result.promptTokens + result.completionTokens + repairTokens
                tokenUsageService.recordAndCheckLimit(total)
                NarrationFormatResult(
                    finalText,
                    result.promptTokens,
                    result.completionTokens + repairTokens
                )
            } catch (retryEx: Exception) {
                log.error("Narration formatting retry failed lang={}—returning original text (flat read). Check OPENAI_API_KEY, rate limits (429). Cause: {}", language, retryEx.message, retryEx)
                NarrationFormatResult(
                    scriptText = sanitizeForNarration(trimLeadingSpecialSymbols(storyText)),
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
            stripLeadingNonLetterUnlessTtsMarker(para)
        }.trim()
    }

    /**
     * Preserve inline TTS markers like [Pause 500ms] at line start; otherwise trim punctuation
     * that sounds awkward when read aloud by TTS.
     */
    private fun stripLeadingNonLetterUnlessTtsMarker(line: String): String {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("[") && trimmed.contains("]")) return trimmed
        return line.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trimStart()
    }

    /**
     * Repair frequent generation artifacts while preserving TTS markers.
     * - "nn" tokens used instead of paragraph breaks
     * - excessive blank lines / spaces
     */
    private fun sanitizeForNarration(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace(Regex("""\bnn\b""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""[^\S\n]{2,}"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    /**
     * Quality gate for user's required outcome:
     * - Fixed: no "nn" artifact
     * - Enhanced: natural paragraph flow
     * - Added: balanced tone + pause modulation markers
     */
    private fun runQualityRepairIfNeeded(
        sourceStory: String,
        draft: String,
        language: String,
        maxTokens: Int
    ): Pair<String, Int>? {
        if (meetsNarrationQuality(draft, language)) return null
        val repairPrompt = """
Rewrite the story to satisfy this quality checklist exactly.

Your original -> Now:
Fixed:
- Remove grammar artifacts and broken tokens (especially "nn")
- Keep continuity (no scene jumps)
- Keep emotional flow
Enhanced:
- Natural spoken style in the output language
- Smooth dialogue transitions
- Habit-building arc through teamwork + effort
- Clear emotional arc
Added:
- Balanced voice modulation markers (tone + pause)
- Audio imagination cues where appropriate
- Strong ending

Rules:
- Preserve plot, characters, and moral from the input.
- Keep marker labels in English.
- Use marker vocabulary already present in the input style.
- Output only the rewritten story text.
        """.trimIndent()

        return try {
            val repaired = rewriteConcurrencyLimiter.withPermit {
                llm.transformWithCustomPrompt(draft.ifBlank { sourceStory }, repairPrompt, maxTokens)
            }
            val cleaned = sanitizeForNarration(trimLeadingSpecialSymbols(repaired.formattedText))
            if (!meetsNarrationQuality(cleaned, language)) {
                log.warn("Narration quality repair attempted but checklist still weak lang={}", language)
                null
            } else {
                val usedTokens = repaired.promptTokens + repaired.completionTokens
                log.info("Narration quality repair applied lang={} tokens={}", language, usedTokens)
                cleaned to usedTokens
            }
        } catch (e: Exception) {
            log.warn("Narration quality repair failed lang={} err={}", language, e.message)
            null
        }
    }

    private fun meetsNarrationQuality(text: String, language: String): Boolean {
        if (text.isBlank()) return false
        if (Regex("""\bnn\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) return false
        val paragraphs = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.size < 4) return false
        if (!pauseMarkerRegex.containsMatchIn(text)) return false
        if (!toneMarkerRegex.containsMatchIn(text)) return false
        if (!meetsLanguageScriptExpectation(text, language)) return false
        return true
    }

    /**
     * Script consistency check for supported languages so "natural spoken style" is enforced broadly.
     * This is intentionally lightweight and only used as a quality gate heuristic.
     */
    private fun meetsLanguageScriptExpectation(text: String, language: String): Boolean {
        val normalized = language.trim().lowercase()
        return when (normalized) {
            "ta", "tamil" -> text.count { it in '\u0B80'..'\u0BFF' } >= 80
            "hi", "hindi" -> text.count { it in '\u0900'..'\u097F' } >= 80
            "te", "telugu" -> text.count { it in '\u0C00'..'\u0C7F' } >= 80
            "kn", "kannada" -> text.count { it in '\u0C80'..'\u0CFF' } >= 80
            "ml", "malayalam" -> text.count { it in '\u0D00'..'\u0D7F' } >= 80
            "en", "english" -> Regex("""[A-Za-z]{120,}""").containsMatchIn(text.replace(" ", ""))
            else -> true
        }
    }
}
