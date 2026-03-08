package com.araro.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TranslationPipelineStatusTest {

    @Test
    fun `isFailed returns true for failure states`() {
        assertTrue(TranslationPipelineStatus.TRANSLATION_FAILED.isFailed())
        assertTrue(TranslationPipelineStatus.REWRITE_FAILED.isFailed())
        assertTrue(TranslationPipelineStatus.TTS_FAILED.isFailed())
    }

    @Test
    fun `isFailed returns false for non-failure states`() {
        assertFalse(TranslationPipelineStatus.PENDING.isFailed())
        assertFalse(TranslationPipelineStatus.TRANSLATING.isFailed())
        assertFalse(TranslationPipelineStatus.REWRITING.isFailed())
        assertFalse(TranslationPipelineStatus.TTS_PROCESSING.isFailed())
        assertFalse(TranslationPipelineStatus.COMPLETED.isFailed())
    }

    @Test
    fun `isTerminal returns true for COMPLETED and failed states`() {
        assertTrue(TranslationPipelineStatus.COMPLETED.isTerminal())
        assertTrue(TranslationPipelineStatus.TRANSLATION_FAILED.isTerminal())
        assertTrue(TranslationPipelineStatus.REWRITE_FAILED.isTerminal())
        assertTrue(TranslationPipelineStatus.TTS_FAILED.isTerminal())
    }

    @Test
    fun `isTerminal returns false for in-progress states`() {
        assertFalse(TranslationPipelineStatus.PENDING.isTerminal())
        assertFalse(TranslationPipelineStatus.TRANSLATING.isTerminal())
        assertFalse(TranslationPipelineStatus.REWRITING.isTerminal())
        assertFalse(TranslationPipelineStatus.TTS_PROCESSING.isTerminal())
    }

    @Test
    fun `canRetry returns true for failed and stuck in-progress states`() {
        assertTrue(TranslationPipelineStatus.TRANSLATION_FAILED.canRetry())
        assertTrue(TranslationPipelineStatus.REWRITE_FAILED.canRetry())
        assertTrue(TranslationPipelineStatus.TTS_FAILED.canRetry())
        assertTrue(TranslationPipelineStatus.REWRITING.canRetry())
        assertTrue(TranslationPipelineStatus.TRANSLATING.canRetry())
        assertTrue(TranslationPipelineStatus.TTS_PROCESSING.canRetry())
        assertFalse(TranslationPipelineStatus.PENDING.canRetry())
        assertFalse(TranslationPipelineStatus.COMPLETED.canRetry())
    }
}
