package com.tamixa.api.library

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.application.storylibrary.StoryLibraryService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.whenever

/**
 * Integration test for LibraryStoryController (categories, list with theme).
 */
class LibraryStoryControllerTest : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var storyLibraryService: StoryLibraryService

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `categories returns 200 with list`() {
        whenever(storyLibraryService.getCategories("ta")).thenReturn(listOf("Adventure", "Animals", "Nature"))

        mockMvc.perform(get("${ApiVersion.V1}/stories/library/categories").param("language", "ta"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories").isArray)
            .andExpect(jsonPath("$.categories.length()").value(3))
            .andExpect(jsonPath("$.categories[0]").value("Adventure"))
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `categories with empty result returns 200`() {
        whenever(storyLibraryService.getCategories("en")).thenReturn(emptyList())

        mockMvc.perform(get("${ApiVersion.V1}/stories/library/categories").param("language", "en"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories").isArray)
            .andExpect(jsonPath("$.categories.length()").value(0))
    }

    @Test
    fun `categories without auth returns 401`() {
        mockMvc.perform(get("${ApiVersion.V1}/stories/library/categories"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `list with theme param passes theme to service`() {
        val story = LibraryStoryResponse(
            id = 1L,
            title = "Test Story",
            content = "",
            theme = "Adventure",
            language = "ta",
            age = 6,
            childName = "Child",
            wordCount = 100,
            readingTimeMinutes = 1.0,
            moral = null,
            audioFileUrl = null,
            status = "PUBLISHED",
            coverImageUrl = null,
            coverVideoUrl = null,
            createdAt = java.time.Instant.now(),
            modifiedAt = java.time.Instant.now(),
            emotionMode = null,
            narrationApprovedAt = java.time.Instant.now()
        )
        whenever(storyLibraryService.findByLanguageApprovedOnly("ta", 0, 50, "Adventure"))
            .thenReturn(org.springframework.data.domain.PageImpl(listOf(story), org.springframework.data.domain.PageRequest.of(0, 50), 1))

        mockMvc.perform(
            get("${ApiVersion.V1}/stories/library")
                .param("language", "ta")
                .param("theme", "Adventure")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.page").value(0))
    }
}
