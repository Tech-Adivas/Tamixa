package com.araro.application.narration

import com.araro.domain.TranslationPipelineStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Idempotency rule: If STORY_AUDIO exists for (storyId + language + voice), skip generation.
 * This test verifies the state logic that supports idempotency.
 */
class StoryProcessingServiceIdempotencyTest {

    @Test
    fun `COMPLETED status indicates audio exists and generation should be skipped`() {
        val status = TranslationPipelineStatus.COMPLETED
        assertTrue(status.isTerminal())
        assertTrue(!status.canRetry())
    }

    @Test
    fun `canRetry is false for COMPLETED - no duplicate retry attempts`() {
        assertTrue(!TranslationPipelineStatus.COMPLETED.canRetry())
    }
}
