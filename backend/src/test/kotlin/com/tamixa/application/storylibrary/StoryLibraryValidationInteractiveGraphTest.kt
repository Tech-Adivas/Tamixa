package com.tamixa.application.storylibrary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoryLibraryValidationInteractiveGraphTest {

    @Test
    fun `normalizeInteractiveGraphJson accepts minimal valid object`() {
        val raw = """{"startSegmentId":"a","segments":{}}"""
        assertEquals(raw, StoryLibraryValidation.normalizeInteractiveGraphJson(raw))
    }

    @Test
    fun `normalizeInteractiveGraphJson returns null for blank`() {
        assertNull(StoryLibraryValidation.normalizeInteractiveGraphJson("  "))
        assertNull(StoryLibraryValidation.normalizeInteractiveGraphJson(null))
    }

    @Test
    fun `normalizeInteractiveGraphJson rejects invalid json`() {
        assertThrows<IllegalArgumentException> {
            StoryLibraryValidation.normalizeInteractiveGraphJson("{")
        }
    }

    @Test
    fun `interactiveGraphHasBranchingPayload false for empty segments`() {
        val raw = """{"startSegmentId":"a","segments":{}}"""
        assertFalse(StoryLibraryValidation.interactiveGraphHasBranchingPayload(raw))
    }

    @Test
    fun `interactiveGraphHasBranchingPayload true when segment present`() {
        val raw = """{"startSegmentId":"intro","segments":{"intro":{"audioUrl":"https://x/a.mp3"}}}"""
        assertTrue(StoryLibraryValidation.interactiveGraphHasBranchingPayload(raw))
    }

    @Test
    fun `validateInteractiveGraphThemeAlignment allows simulator theme with graph`() {
        val raw = """{"startSegmentId":"intro","segments":{"intro":{"audioUrl":"https://x/a.mp3"}}}"""
        StoryLibraryValidation.validateInteractiveGraphThemeAlignment("Learn · Simulator · Digital Safety", null, raw)
    }

    @Test
    fun `validateInteractiveGraphThemeAlignment allows graph when category is simulator`() {
        val raw = """{"startSegmentId":"intro","segments":{"intro":{"audioUrl":"https://x/a.mp3"}}}"""
        StoryLibraryValidation.validateInteractiveGraphThemeAlignment("Adventure", "Learn · Simulator · Digital Safety", raw)
    }

    @Test
    fun `validateInteractiveGraphThemeAlignment rejects Learn Digital Safety without Simulator`() {
        val raw = """{"startSegmentId":"intro","segments":{"intro":{"audioUrl":"https://x/a.mp3"}}}"""
        assertThrows<IllegalArgumentException> {
            StoryLibraryValidation.validateInteractiveGraphThemeAlignment("Learn · Digital Safety", null, raw)
        }
    }
}
