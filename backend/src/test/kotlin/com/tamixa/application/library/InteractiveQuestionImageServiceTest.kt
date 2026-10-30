package com.tamixa.application.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.port.ImageStoragePort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.LibraryStoryStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class InteractiveQuestionImageServiceTest {

    private val mapper = ObjectMapper()
    private val repository: StoryLibraryRepositoryPort = mock()
    private val imageGeneration: ImageGenerationPort = mock()
    private val imageStorage: ImageStoragePort = mock()
    private val service = InteractiveQuestionImageService(repository, imageGeneration, imageStorage, mapper)

    private val graph = """
        {"startSegmentId":"hook","segments":{
          "hook":{"text":"Meena looks at the sky. இப்போது அவள் என்ன செய்ய வேண்டும்?","audioUrl":"a",
                  "choices":[{"id":"c1","label":"Fly","nextSegmentId":"mid"},{"id":"c2","label":"Wait","nextSegmentId":"end"}]},
          "mid":{"text":"She flies. Left or right?","audioUrl":"b",
                 "choices":[{"id":"c3","label":"Left","nextSegmentId":"end"}]},
          "end":{"text":"The end.","audioUrl":"c","choices":[]}}}
    """.trimIndent()

    private fun story(graphJson: String? = graph) = LibraryStory(
        id = 7,
        title = "Sky Mission",
        content = "Story",
        theme = "adventure",
        category = "adventure",
        language = "ta",
        age = 8,
        childName = "Child",
        wordCount = 100,
        readingTimeMinutes = 5.0,
        moral = null,
        audioFileUrl = null,
        status = LibraryStoryStatus.PUBLISHED,
        coverImageUrl = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        interactiveGraphJson = graphJson,
    )

    @Test
    fun generatesOneImagePerQuestion_andSavesUrlsToGraph() {
        whenever(repository.findById(7)).thenReturn(story())
        whenever(imageGeneration.generateImage(any())).thenReturn(byteArrayOf(1, 2, 3))
        whenever(imageStorage.storeInteractiveSegmentImage(eq(7L), any(), any()))
            .thenAnswer { "interactive_images/7/${it.getArgument<String>(1)}.png" }

        val result = service.generateQuestionImages(7, null, null)

        assertEquals(listOf("hook", "mid"), result.generated.map { it.segmentId })
        assertTrue(result.failed.isEmpty())
        val saved = argumentCaptor<LibraryStory>()
        verify(repository).update(saved.capture())
        val segs = mapper.readTree(saved.firstValue.interactiveGraphJson).path("segments")
        assertTrue(segs.path("hook").path("imageUrl").asText().startsWith("/api/v1/covers/interactive_images/7/hook.png?v="))
        assertTrue(segs.path("mid").path("imageUrl").asText().startsWith("/api/v1/covers/interactive_images/7/mid.png?v="))
        assertFalse(segs.path("end").has("imageUrl"), "segments without a question get no picture")
        assertEquals(LibraryStoryStatus.PUBLISHED, saved.firstValue.status)
    }

    @Test
    fun promptKeepsTamilVowelSigns() {
        whenever(repository.findById(7)).thenReturn(story())
        whenever(imageGeneration.generateImage(any())).thenReturn(byteArrayOf(1))
        whenever(imageStorage.storeInteractiveSegmentImage(any(), any(), any())).thenReturn("interactive_images/7/hook.png")

        service.generateQuestionImages(7, listOf("hook"), "warm light")

        val prompt = argumentCaptor<String>()
        verify(imageGeneration).generateImage(prompt.capture())
        assertTrue(prompt.firstValue.contains("இப்போது அவள் என்ன செய்ய வேண்டும்?"))
        assertTrue(prompt.firstValue.contains("warm light"))
    }

    @Test
    fun retriesWithFallbackPrompt_thenReportsFailureWithoutSaving() {
        whenever(repository.findById(7)).thenReturn(story())
        whenever(imageGeneration.generateImage(any())).thenReturn(null)

        val result = service.generateQuestionImages(7, listOf("hook"), null)

        verify(imageGeneration, times(2)).generateImage(any())
        assertEquals(listOf("hook"), result.failed.map { it.segmentId })
        verify(repository, never()).update(any())
    }

    @Test
    fun writesIntoTheLatestSavedGraph() {
        val editedMeanwhile = graph.replace("The end.", "The happy end.")
        whenever(repository.findById(7)).thenReturn(story(), story(editedMeanwhile))
        whenever(imageGeneration.generateImage(any())).thenReturn(byteArrayOf(1))
        whenever(imageStorage.storeInteractiveSegmentImage(any(), any(), any())).thenReturn("interactive_images/7/hook.png")

        service.generateQuestionImages(7, listOf("hook"), null)

        val saved = argumentCaptor<LibraryStory>()
        verify(repository).update(saved.capture())
        val segs = mapper.readTree(saved.firstValue.interactiveGraphJson).path("segments")
        assertEquals("The happy end.", segs.path("end").path("text").asText())
        assertTrue(segs.path("hook").has("imageUrl"))
    }

    @Test
    fun rejectsStoryWithoutQuestions() {
        whenever(repository.findById(7)).thenReturn(
            story("""{"startSegmentId":"a","segments":{"a":{"text":"x","audioUrl":"","choices":[]}}}""")
        )
        assertThrows<IllegalArgumentException> { service.generateQuestionImages(7, null, null) }
        assertThrows<IllegalArgumentException> {
            whenever(repository.findById(7)).thenReturn(story())
            service.generateQuestionImages(7, listOf("end"), null)
        }
    }
}
