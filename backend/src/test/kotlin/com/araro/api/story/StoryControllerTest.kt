package com.araro.api.story

import com.araro.IntegrationTestBase
import com.araro.api.ApiVersion
import com.araro.api.story.dto.GenerateStoryRequest
import com.araro.application.story.StoryService
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import com.araro.application.curated.CuratedStoryService
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
    private lateinit var curatedStoryService: CuratedStoryService

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `search with auth returns 200`() {
        whenever(curatedStoryService.search(any(), any(), any(), any())).thenReturn(PageImpl(emptyList(), PageRequest.of(0, 20), 0))
        whenever(storyService.search(any(), any(), any(), any())).thenReturn(PageImpl(emptyList(), PageRequest.of(0, 20), 0))

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
            language = "en",
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
            language = "en",
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
            storyService.generate(any(), any(), any(), any(), any(), any(), any(), any())
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

        verify(storyService).generate(any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `generate without auth returns 401`() {
        val request = GenerateStoryRequest(
            age = 6,
            language = "en",
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
}
