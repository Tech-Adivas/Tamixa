package com.tamixa.application.port

import com.tamixa.domain.StoryVersion

interface StoryVersionRepositoryPort {

    /** Create a version snapshot before updating library story. Returns the saved version. */
    fun createVersion(
        libraryStoryId: Long,
        title: String?,
        content: String,
        theme: String,
        moral: String?,
        wordCount: Int,
        readingTimeMinutes: Double,
        createdBy: String? = null
    ): StoryVersion

    /** Next version number for a story (max + 1, or 1 if none). */
    fun nextVersionNumber(libraryStoryId: Long): Int
}
