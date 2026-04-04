package com.tamixa.application.story

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoverIllustrationPromptsTest {

    @Test
    fun `buildChildSafeCoverPrompt includes scene text and safety wrapper`() {
        val scene = "A gentle fox shares berries with a rabbit in a moonlit garden."
        val prompt = CoverIllustrationPrompts.buildChildSafeCoverPrompt(scene)
        assertTrue(prompt.contains("child-safe", ignoreCase = true))
        assertTrue(prompt.contains("fox"))
        assertTrue(prompt.contains("No text", ignoreCase = true))
    }

    @Test
    fun `buildChildSafeCoverPrompt strips unusual characters from scene`() {
        val prompt = CoverIllustrationPrompts.buildChildSafeCoverPrompt("Hello<>{}#emoji")
        assertTrue(!prompt.contains("<"))
        assertTrue(!prompt.contains(">"))
    }
}
