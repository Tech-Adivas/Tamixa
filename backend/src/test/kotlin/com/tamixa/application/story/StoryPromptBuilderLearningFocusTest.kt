package com.tamixa.application.story

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Regression tests for optional [learningFocus] lines in user prompts. */
class StoryPromptBuilderLearningFocusTest {

    private val builder = StoryPromptBuilder()

    @Test
    fun `buildUserPrompt includes public speaking hint`() {
        val prompt = builder.buildUserPrompt(
            age = 7,
            language = "ta",
            theme = "school assembly",
            childName = "Mira",
            learningFocus = "public_speaking"
        )
        assertTrue(prompt.contains("Public speaking"), prompt)
    }

    @Test
    fun `buildUserPrompt includes money literacy hint`() {
        val prompt = builder.buildUserPrompt(
            age = 8,
            language = "ta",
            theme = "market day",
            childName = "Kavi",
            learningFocus = "money_literacy"
        )
        assertTrue(prompt.contains("Money literacy"), prompt)
    }

    @Test
    fun `buildUserPrompt includes research skills hint`() {
        val prompt = builder.buildUserPrompt(
            age = 9,
            language = "ta",
            theme = "library mystery",
            childName = "Arun",
            learningFocus = "research_skills"
        )
        assertTrue(prompt.contains("Research and curiosity"), prompt)
    }

    @Test
    fun `buildUserPrompt ignores unknown learningFocus`() {
        val prompt = builder.buildUserPrompt(
            age = 6,
            language = "ta",
            theme = "forest",
            childName = "Lee",
            learningFocus = "not_a_real_focus"
        )
        assertFalse(prompt.contains("Learning focus:"), prompt)
    }

    @Test
    fun `buildFallbackUserPrompt passes through learningFocus`() {
        val prompt = builder.buildFallbackUserPrompt(
            age = 8,
            language = "ta",
            theme = "friends",
            childName = "Sam",
            learningFocus = "research_skills"
        )
        assertTrue(prompt.contains("Research and curiosity"), prompt)
    }
}
