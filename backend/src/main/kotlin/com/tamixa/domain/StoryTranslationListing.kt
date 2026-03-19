package com.tamixa.domain

import java.time.Instant

/**
 * Lightweight translation listing (projection).
 */
data class StoryTranslationListing(
    val id: Long,
    val masterStoryId: Long,
    val title: String?,
    val language: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val createdAt: Instant
)
