package com.araro.application.story

import com.araro.application.port.OpenAIPort
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.ApplicationMetrics
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Multi-layer content moderation before saving a story.
 * Layers run in order; first failure wins. Logs which layer triggered rejection.
 *
 * Layer 1: OpenAI Moderation API
 * Layer 2: Custom configurable keyword blocklist
 * Layer 3: Regex-based red-flag pattern detection
 * Layer 4: Age-based vocabulary restriction (e.g. no fear words for age < 6)
 */
@Service
class StoryModerationService(
    private val openAI: OpenAIPort,
    private val metrics: ApplicationMetrics,
    private val appProperties: AppProperties,
    private val safetyValidationRules: StorySafetyValidationRules
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Layer 3: Regex patterns for red-flag content (political, religious conflict, violence hints). */
    private val redFlagPatterns = listOf(
        Regex("""(?i)\b(president|minister|parliament|election|vote|party|politic)\b"""),
        Regex("""(?i)\b(religious\s+conflict|sectarian|jihad|crusade)\b"""),
        Regex("""(?i)\b(kill|murder|suicide|blood\s+shed|war\s+crimes)\b"""),
        Regex("""(?i)\b(weapon|rifle|bomb|explosive)\b""")
    )

    private fun customBlocklist(): Set<String> {
        val configured = appProperties.story.keywordBlocklist
        if (configured.isBlank()) return emptySet()
        return configured.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    /**
     * Runs all moderation layers on [text]. Rejects (throws) if any layer flags content.
     * Logs which layer triggered rejection for safety auditing.
     */
    fun moderateBeforeSave(
        text: String,
        context: ModerationContext
    ) {
        // Layer 1: OpenAI Moderation API
        val openaiResult = openAI.getModerationResult(text)
        logModerationResult(context, openaiResult)
        if (!openaiResult.safe || openaiResult.categories.anyFlagged()) {
            metrics.recordModerationFailure(ModerationLayer.OPENAI_API)
            logRejection(context, ModerationLayer.OPENAI_API)
            throw ContentModerationException("Generated story failed content moderation")
        }

        // Layer 2: Custom keyword blocklist (configurable)
        val blocklist = customBlocklist()
        if (blocklist.isNotEmpty()) {
            val lower = text.lowercase()
            blocklist.find { lower.contains(it) }?.let { matched ->
                metrics.recordModerationFailure(ModerationLayer.CUSTOM_KEYWORD_BLOCKLIST)
                logRejection(context, ModerationLayer.CUSTOM_KEYWORD_BLOCKLIST, matched)
                throw ContentModerationException("Generated story failed content moderation")
            }
        }

        // Layer 3: Pattern detection (regex red flags)
        redFlagPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(text)) {
                metrics.recordModerationFailure(ModerationLayer.PATTERN_DETECTION)
                logRejection(context, ModerationLayer.PATTERN_DETECTION, pattern.pattern)
                throw ContentModerationException("Generated story failed content moderation")
            }
        }

        // Layer 4: Age-based vocabulary restriction
        try {
            safetyValidationRules.validate(text, context.age)
        } catch (e: ContentModerationException) {
            metrics.recordModerationFailure(ModerationLayer.AGE_VOCABULARY)
            logRejection(context, ModerationLayer.AGE_VOCABULARY)
            throw e
        }
    }

    private fun logModerationResult(context: ModerationContext, result: ModerationResult) {
        log.info(
            "story_moderation",
            StructuredArguments.kv("promptId", context.promptId),
            StructuredArguments.kv("language", context.language),
            StructuredArguments.kv("age", context.age),
            StructuredArguments.kv("moderationSafe", result.safe),
            StructuredArguments.kv("categoriesFlagged", result.categories.anyFlagged())
        )
    }

    private fun logRejection(context: ModerationContext, layer: ModerationLayer, detail: String? = null) {
        log.warn(
            "moderation_rejection",
            StructuredArguments.kv("promptId", context.promptId),
            StructuredArguments.kv("layer", layer.name),
            StructuredArguments.kv("age", context.age),
            StructuredArguments.kv("detail", detail)
        )
    }
}

data class ModerationContext(
    val promptId: String,
    val language: String,
    val age: Int
)
