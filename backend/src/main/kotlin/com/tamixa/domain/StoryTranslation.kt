package com.tamixa.domain

import java.time.Instant

data class StoryTranslation(
    val id: Long,
    val masterStoryId: Long,
    val language: String,
    val title: String?,
    val content: String,
    val moral: String?,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val createdAt: Instant,
    val status: TranslationPipelineStatus = TranslationPipelineStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val narrationApprovedAt: Instant? = null
)
