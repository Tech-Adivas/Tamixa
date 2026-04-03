package com.tamixa.api.story

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import com.tamixa.api.story.dto.GenerateStoryRequest
import com.tamixa.application.story.StoryService
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.application.storylibrary.StoryLibraryService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * Integration test for StoryController.
 * Extends IntegrationTestBase for full Spring context (requires Testcontainers/Docker for DB).
 */
class StoryControllerTest : IntegrationTestBase() {

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var mockMvc: MockMvc

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var storyService: StoryService

    @MockBean
    private lateinit var storyLibraryService: StoryLibraryService

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `search with auth returns 200`() {
        whenever(storyLibraryService.search(any(), any(), any(), any())).thenReturn(PageImpl(emptyList<LibraryStoryResponse>(), PageRequest.of(0, 20), 0))
        whenever(storyService.search(any(), any(), any(), any())).thenReturn(PageImpl(emptyList<Story>(), PageRequest.of(0, 20), 0))

        mockMvc.perform(get("${ApiVersion.V1}/stories/search").param("q", "fox"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `search without auth returns 401`() {
        mockMvc.perform(get("${ApiVersion.V1}/stories/search").param("q", "fox"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `generate story returns 201 and story response`() {
        val request = GenerateStoryRequest(
            age = 6,
            language = "ta",
            theme = "dinosaurs",
            childName = "Alex",
            childId = null
        )
        val savedStory = Story(
            id = 1L,
            parentId = 100L,
            childId = null,
            content = "Once upon a time...",
            theme = "dinosaurs",
            language = "ta",
            age = 6,
            childName = "Alex",
            wordCount = 50,
            readingTimeMinutes = 0.33,
            title = "The Dinosaur",
            moral = "Be kind.",
            status = StoryStatus.PENDING,
            audioFileUrl = null,
            createdAt = Instant.now(),
            safetyScore = 85
        )
        whenever(
            storyService.generate(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
            )
        ).thenReturn(savedStory)

        mockMvc.perform(
            post("${ApiVersion.V1}/stories/generate")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.theme").value("dinosaurs"))
            .andExpect(jsonPath("$.status").value("PENDING"))

        verify(storyService).generate(
            any(),
            any(),
            any(),
            any(),
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(false),
        )
    }

    @Test
    fun `generate without auth returns 401`() {
        val request = GenerateStoryRequest(
            age = 6,
            language = "ta",
            theme = "dinosaurs",
            childName = "Alex",
            childId = null
        )
        mockMvc.perform(
            post("${ApiVersion.V1}/stories/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `generation topics returns 200 with catalog entries`() {
        mockMvc.perform(get("${ApiVersion.V1}/stories/generation-topics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].theme").exists())
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `generate with generationTopicId only returns 201 and uses trusted catalog theme`() {
        val pongalTheme = "பொங்கல் — நன்றியுணர்வும் புதிய தொடக்கமும்"
        val body = mapOf(
            "age" to 8,
            "language" to "ta",
            "generationTopicId" to "pongal_gratitude",
        )
        val savedStory = Story(
            id = 2L,
            parentId = 100L,
            childId = null,
            content = "…",
            theme = pongalTheme,
            language = "ta",
            age = 8,
            childName = "Listener",
            wordCount = 40,
            readingTimeMinutes = 0.25,
            title = "Pongal",
            moral = null,
            status = StoryStatus.PENDING,
            audioFileUrl = null,
            createdAt = Instant.now(),
            safetyScore = 90
        )
        whenever(
            storyService.generate(
                any(),
                eq(8),
                eq("ta"),
                eq(pongalTheme),
                eq("Listener"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("curiosity"),
                eq(true),
            )
        ).thenReturn(savedStory)

        mockMvc.perform(
            post("${ApiVersion.V1}/stories/generate")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(2))

        verify(storyService).generate(
            any(),
            eq(8),
            eq("ta"),
            eq(pongalTheme),
            eq("Listener"),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq("curiosity"),
            eq(true),
        )
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `generate with unknown generationTopicId returns 400`() {
        val body = mapOf(
            "age" to 8,
            "language" to "ta",
            "generationTopicId" to "not_a_real_topic_id",
        )
        mockMvc.perform(
            post("${ApiVersion.V1}/stories/generate")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("UNKNOWN_GENERATION_TOPIC"))
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `generate without theme or topic returns 400`() {
        val body = mapOf(
            "age" to 8,
            "language" to "ta",
        )
        mockMvc.perform(
            post("${ApiVersion.V1}/stories/generate")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("THEME_OR_TOPIC_REQUIRED"))
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `generate with unsupported language returns 400 with structured code`() {
        val body = mapOf(
            "age" to 8,
            "language" to "en",
            "theme" to "forest",
        )
        mockMvc.perform(
            post("${ApiVersion.V1}/stories/generate")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GENERATION_LANGUAGE_NOT_SUPPORTED"))
    }
}
