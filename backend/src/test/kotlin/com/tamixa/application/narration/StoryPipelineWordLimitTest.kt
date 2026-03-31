package com.tamixa.application.narration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StoryPipelineWordLimitTest {

    @Test
    fun `clamp leaves short text unchanged`() {
        val t = "one two three four five"
        assertEquals(t, StoryPipelineWordLimit.clampToMaxWords(t, 100))
    }

    @Test
    fun `clamp truncates to max words`() {
        val t = (1..50).joinToString(" ") { "w$it" }
        val out = StoryPipelineWordLimit.clampToMaxWords(t, 10)
        assertEquals(10, out.split(Regex("\\s+")).filter { it.isNotBlank() }.size)
    }
}
