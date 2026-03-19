package com.tamixa.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStoryResponseTest {

    @Test
    fun toStory_mapsThemeAsTitleWhenTitleBlank() {
        val response = LibraryStoryResponse(
            id = 1L,
            title = "",
            content = "Story content",
            theme = "Animals",
            category = null,
            language = "ta",
            age = 6,
            childName = "Listener",
            wordCount = 100,
            readingTimeMinutes = 0.5,
            moral = null,
            audioFileUrl = null,
            coverImageUrl = null,
            coverVideoUrl = null,
            createdAt = "2025-01-01T00:00:00Z"
        )
        val story = response.toStory()
        assertEquals("Animals", story.theme)
    }

    @Test
    fun toStory_prefersTitleWhenNotBlank() {
        val response = LibraryStoryResponse(
            id = 2L,
            title = "The Fox and the Grapes",
            content = "A fox...",
            theme = "Animals",
            category = "animals",
            language = "ta",
            age = 6,
            childName = "Listener",
            wordCount = 50,
            readingTimeMinutes = 0.33,
            moral = null,
            audioFileUrl = null,
            coverImageUrl = null,
            coverVideoUrl = null,
            createdAt = "2025-01-01T00:00:00Z"
        )
        val story = response.toStory()
        assertEquals("The Fox and the Grapes", story.theme)
    }
}
