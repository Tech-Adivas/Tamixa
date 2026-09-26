package com.tamixa.application.storylibrary

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InteractiveGraphPlaybackPathRewriteTest {

    private val mapper = ObjectMapper()

    @Test
    fun rewriteUrl_swapsLanguageSegment() {
        val u = "http://localhost:8080/audio/stories/86/ta/interactive_intro_default?v=1"
        assertEquals(
            "http://localhost:8080/audio/stories/86/en/interactive_intro_default?v=1",
            InteractiveGraphPlaybackPathRewrite.rewriteStoriesPathLanguageInUrl(u, 86L, "ta", "en"),
        )
    }

    @Test
    fun rewriteUrl_caseInsensitiveFromLang() {
        val u = "https://x/audio/stories/5/TA/foo"
        assertEquals(
            "https://x/audio/stories/5/en/foo",
            InteractiveGraphPlaybackPathRewrite.rewriteStoriesPathLanguageInUrl(u, 5L, "ta", "en"),
        )
    }

    @Test
    fun rewriteUrl_unchangedWhenNoMatchingPath() {
        val u = "https://cdn.example.com/audio.mp3"
        assertEquals(u, InteractiveGraphPlaybackPathRewrite.rewriteStoriesPathLanguageInUrl(u, 86L, "ta", "en"))
    }

    @Test
    fun extractStoriesStorageKey_handlesFullAndBarePaths() {
        assertEquals(
            "stories/10/ta/x",
            InteractiveGraphPlaybackPathRewrite.extractStoriesStorageKey("http://h/audio/stories/10/ta/x?v=1"),
        )
        assertEquals("stories/5/en/a", InteractiveGraphPlaybackPathRewrite.extractStoriesStorageKey("stories/5/en/a"))
        assertNull(InteractiveGraphPlaybackPathRewrite.extractStoriesStorageKey("https://cdn/x.mp3"))
    }

    @Test
    fun rewriteGraph_updatesSegmentAudioUrlsWhenTargetKeyExists() {
        val root = mapper.readTree(
            """
            {
              "startSegmentId": "a",
              "segments": {
                "a": { "audioUrl": "http://h/audio/stories/10/ta/interactive_a_default", "choices": [] }
              }
            }
            """.trimIndent(),
        )
        val out = InteractiveGraphPlaybackPathRewrite.rewriteSegmentAudioStorageLanguage(
            root,
            10L,
            "ta",
            "en",
            storageKeyExists = { true },
        )
        assertTrue(out.isObject)
        assertEquals(
            "http://h/audio/stories/10/en/interactive_a_default",
            out.path("segments").path("a").path("audioUrl").asText(),
        )
    }

    @Test
    fun rewriteGraph_keepsMasterUrlsWhenTargetKeyMissing() {
        val root = mapper.readTree(
            """
            {
              "startSegmentId": "a",
              "segments": {
                "a": { "audioUrl": "http://h/audio/stories/10/ta/interactive_a_default", "choices": [] }
              }
            }
            """.trimIndent(),
        )
        val out = InteractiveGraphPlaybackPathRewrite.rewriteSegmentAudioStorageLanguage(
            root,
            10L,
            "ta",
            "en",
            storageKeyExists = { false },
        )
        assertEquals(
            "http://h/audio/stories/10/ta/interactive_a_default",
            out.path("segments").path("a").path("audioUrl").asText(),
        )
    }
}
