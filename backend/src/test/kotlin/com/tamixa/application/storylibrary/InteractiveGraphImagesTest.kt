package com.tamixa.application.storylibrary

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class InteractiveGraphImagesTest {

    private val mapper = ObjectMapper()

    private val stored = """
        {"startSegmentId":"hook","segments":{
          "hook":{"text":"Now what?","audioUrl":"a","imageUrl":"/api/v1/covers/interactive_images/7/hook.png?v=1",
                  "choices":[{"id":"c1","label":"Go","nextSegmentId":"end"}]},
          "end":{"text":"Done.","audioUrl":"b","choices":[]}}}
    """.trimIndent()

    @Test
    fun preserve_keepsStoredImage_whenIncomingSegmentHasNoImageUrlKey() {
        val incoming = """{"startSegmentId":"hook","segments":{"hook":{"text":"Now what? Edited","audioUrl":"a",
            "choices":[{"id":"c1","label":"Go","nextSegmentId":"end"}]},"end":{"text":"Done.","audioUrl":"b","choices":[]}}}"""
        val out = mapper.readTree(InteractiveGraphImages.preserveImageUrls(incoming, stored))
        assertEquals("/api/v1/covers/interactive_images/7/hook.png?v=1", out.path("segments").path("hook").path("imageUrl").asText())
        assertEquals("Now what? Edited", out.path("segments").path("hook").path("text").asText())
        assertFalse(out.path("segments").path("end").has("imageUrl"))
    }

    @Test
    fun preserve_explicitBlankImageUrlRemovesIt() {
        val incoming = """{"startSegmentId":"hook","segments":{"hook":{"text":"x","audioUrl":"a","imageUrl":"",
            "choices":[{"id":"c1","label":"Go","nextSegmentId":"end"}]},"end":{"text":"y","audioUrl":"b","choices":[]}}}"""
        assertSame(incoming, InteractiveGraphImages.preserveImageUrls(incoming, stored))
    }

    @Test
    fun preserve_nullOrNoStoredImages_returnsIncomingUnchanged() {
        assertEquals(null, InteractiveGraphImages.preserveImageUrls(null, stored))
        val incoming = """{"segments":{"hook":{"text":"x"}}}"""
        assertSame(incoming, InteractiveGraphImages.preserveImageUrls(incoming, null))
    }

    @Test
    fun withMasterImageUrls_fillsTranslationSegmentsBySharedId() {
        val translation = mapper.readTree(
            """{"startSegmentId":"hook","segments":{"hook":{"text":"What now?","audioUrl":"en-a",
               "choices":[{"id":"c1","label":"Go","nextSegmentId":"end"}]},"end":{"text":"Done","audioUrl":"en-b","choices":[]}}}"""
        )
        val out = InteractiveGraphImages.withMasterImageUrls(translation, stored)
        assertEquals("/api/v1/covers/interactive_images/7/hook.png?v=1", out.path("segments").path("hook").path("imageUrl").asText())
        assertEquals("What now?", out.path("segments").path("hook").path("text").asText())
        assertFalse(translation.path("segments").path("hook").has("imageUrl"), "input graph must not be mutated")
    }

    @Test
    fun withMasterImageUrls_keepsTranslationsOwnImage() {
        val translation = mapper.readTree("""{"segments":{"hook":{"imageUrl":"/api/v1/covers/interactive_images/7/own.png"}}}""")
        val out = InteractiveGraphImages.withMasterImageUrls(translation, stored)
        assertEquals("/api/v1/covers/interactive_images/7/own.png", out.path("segments").path("hook").path("imageUrl").asText())
    }
}
