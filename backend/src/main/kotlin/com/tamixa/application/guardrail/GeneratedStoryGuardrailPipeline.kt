package com.tamixa.application.guardrail

import com.tamixa.application.port.StructuredStoryRemoteGuardrailPort
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.story.ModerationContext
import com.tamixa.application.story.StoryModerationService
import com.tamixa.application.story.StoryPromptBuilder
import com.tamixa.application.story.StorySafetyMiddleware
import com.tamixa.application.story.StorySafetyScoreService
import com.tamixa.application.story.StructuredStoryPayload
import com.tamixa.infrastructure.config.AppProperties
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Single orchestration point for **all** post-LLM checks on a structured parent-generated story.
 * Order is fixed; the first failure throws [ContentModerationException].
 */
@Service
class GeneratedStoryGuardrailPipeline(
    private val storyModeration: StoryModerationService,
    private val safetyMiddleware: StorySafetyMiddleware,
    private val storySafetyScore: StorySafetyScoreService,
    private val storyPromptBuilder: StoryPromptBuilder,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val remoteStructuredStoryGuardrail: StructuredStoryRemoteGuardrailPort? = null
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Runs moderation layers, static blocklist, and safety score. Returns the score for persistence.
     */
    fun enforceStructuredStoryPayload(
        payload: StructuredStoryPayload,
        context: ModerationContext
    ): Int {
        remoteStructuredStoryGuardrail?.let { port ->
            logStage(GuardrailStage.STORY_POST_REMOTE_SERVICE, context)
            val maxWords = storyPromptBuilder.maxWordsForAge(context.age).coerceAtMost(appProperties.story.maxWords)
            port.validateStructuredStory(payload, context, maxWords)
        }

        logStage(GuardrailStage.STORY_POST_FULL_MODERATION, context)
        storyModeration.moderateBeforeSave(payload.storyText, context)

        logStage(GuardrailStage.STORY_POST_STATIC_BLOCKLIST, context)
        if (!safetyMiddleware.isGeneratedContentChildSafe(payload.storyText)) {
            throw ContentModerationException("Generated story contains disallowed vocabulary")
        }

        logStage(GuardrailStage.STORY_POST_SAFETY_SCORE, context)
        return storySafetyScore.computeAndValidate(payload)
    }

    private fun logStage(stage: GuardrailStage, ctx: ModerationContext) {
        log.debug(
            "guardrail_stage",
            kv("guardrailStage", stage.name),
            kv("promptId", ctx.promptId),
            kv("language", ctx.language),
            kv("age", ctx.age)
        )
    }
}
