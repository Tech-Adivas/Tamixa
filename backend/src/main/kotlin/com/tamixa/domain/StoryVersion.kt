package com.tamixa.domain

import java.time.Instant

/**
 * Snapshot of library story content at a point in time.
 * Created when content is edited; library_stories holds current.
 */
data class StoryVersion(
    val id: Long,
    val libraryStoryId: Long,
    val versionNumber: Int,
    val title: String?,
    val content: String,
    val theme: String,
    val moral: String?,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val createdAt: Instant,
    val createdBy: String? = null
)
