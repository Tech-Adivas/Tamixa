package com.tamixa.api.story.dto

import com.tamixa.domain.StoryStatus
import java.time.Instant

data class StoryResponse(
    val id: Long,
    val parentId: Long,
    val childId: Long?,
    val content: String,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val title: String?,
    val moral: String?,
    val status: StoryStatus,
    val audioFileUrl: String?,
    val createdAt: Instant,
    val coverImageUrl: String? = null,
    /** Animated GIF cover when generated. Shown when available. */
    val coverVideoUrl: String? = null
)
