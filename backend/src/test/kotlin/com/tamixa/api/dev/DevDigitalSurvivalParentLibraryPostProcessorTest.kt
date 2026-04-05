package com.tamixa.api.dev

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.api.admin.dto.LibraryStoryResponse
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DevDigitalSurvivalParentLibraryPostProcessorTest {

    private val mapper = ObjectMapper()
    private val processor = DevDigitalSurvivalParentLibraryPostProcessorImpl(mapper)

    @Test
    fun `rewrites digital survival CDN segment urls for seed owner`() {
        val graphJson =
            """
            {"startSegmentId":"ep03_hook","segments":{"ep03_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep03/en/hook.mp3","choices":[]}}}
            """.trimIndent()
        val node = mapper.readTree(graphJson)
        val response =
            LibraryStoryResponse(
                id = 1L,
                title = "T",
                content = "c",
                theme = "Learn · Simulator · Digital Safety",
                language = "en",
                age = 10,
                childName = "Child",
                wordCount = 10,
                readingTimeMinutes = 1.0,
                moral = null,
                audioFileUrl = "/x",
                status = "PUBLISHED",
                coverImageUrl = null,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
                storyOwner = DevDigitalSurvivalParentLibraryPostProcessorImpl.DIGITAL_SURVIVAL_STORY_OWNER,
                interactiveGraph = node,
            )
        val out = processor.apply(response)
        val url =
            out.interactiveGraph?.path("segments")?.path("ep03_hook")?.path("audioUrl")?.asText().orEmpty()
        assertEquals(DevDigitalSurvivalParentLibraryPostProcessorImpl.PLACEHOLDER_AUDIO_PATH, url)
    }

    @Test
    fun `no rewrite for other story owner`() {
        val graphJson =
            """{"startSegmentId":"x","segments":{"x":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/h.mp3","choices":[]}}}"""
        val node = mapper.readTree(graphJson)
        val response =
            LibraryStoryResponse(
                id = 2L,
                title = "T",
                content = "c",
                theme = "Learn · Simulator · Digital Safety",
                language = "en",
                age = 10,
                childName = "Child",
                wordCount = 10,
                readingTimeMinutes = 1.0,
                moral = null,
                audioFileUrl = "/x",
                status = "PUBLISHED",
                coverImageUrl = null,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
                storyOwner = "other:owner",
                interactiveGraph = node,
            )
        val out = processor.apply(response)
        assertTrue(out.interactiveGraph.toString().contains("cdn.tamixa.app"))
    }
}
