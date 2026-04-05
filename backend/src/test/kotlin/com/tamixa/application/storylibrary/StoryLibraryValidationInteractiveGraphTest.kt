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

    private fun assertDigitalSurvivalFixture(resourcePath: String) {
        val raw = javaClass.getResourceAsStream(resourcePath)!!.bufferedReader().readText().trim()
        assertEquals(raw, StoryLibraryValidation.normalizeInteractiveGraphJson(raw))
        assertTrue(StoryLibraryValidation.interactiveGraphHasBranchingPayload(raw))
        StoryLibraryValidation.validateInteractiveGraphThemeAlignment(
            "Learn · Simulator · Digital Safety",
            "Learn · Simulator · Digital Safety",
            raw,
        )
    }

    /**
     * Fixtures generated with [backend/scripts/generate_digital_survival_guide_seed_sql.py]
     * (same interactive_graph payloads as Flyway V86–V90 English pilot rows).
     */
    @Test
    fun `digital survival guide ep01 english fixture graph validates and matches simulator theme`() {
        assertDigitalSurvivalFixture("/edu/digital_survival_guide_ep01_en.graph.json")
    }

    @Test
    fun `digital survival guide ep02 kyc english fixture graph validates and matches simulator theme`() {
        assertDigitalSurvivalFixture("/edu/digital_survival_guide_ep02_en.graph.json")
    }

    @Test
    fun `digital survival guide ep03 through ep15 english fixture graphs validate and match simulator theme`() {
        for (n in 3..15) {
            assertDigitalSurvivalFixture("/edu/digital_survival_guide_ep${String.format("%02d", n)}_en.graph.json")
        }
    }
}
