package com.tamixa.application.guardrail

/**
 * Named stages for structured logging and tracing across the guardrail pipeline.
 * See [GeneratedStoryGuardrailPipeline].
 */
enum class GuardrailStage {
    /** Optional HTTP service (separate repo) — structural / shared policy before Kotlin moderation. */
    STORY_POST_REMOTE_SERVICE,

    /** OpenAI Moderations + keyword + regex + age vocabulary ([com.tamixa.application.story.StoryModerationService]). */
    STORY_POST_FULL_MODERATION,

    /** Static child blocklist on story body ([com.tamixa.application.story.StorySafetyMiddleware]). */
    STORY_POST_STATIC_BLOCKLIST,

    /** Heuristic 0–100 score vs threshold ([com.tamixa.application.story.StorySafetyScoreService]). */
    STORY_POST_SAFETY_SCORE
}
