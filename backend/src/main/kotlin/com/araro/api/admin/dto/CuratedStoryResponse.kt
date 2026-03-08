package com.araro.api.admin.dto

import java.time.Instant

data class CuratedStoryResponse(
    val id: Long,
    val title: String?,
    val content: String,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val moral: String?,
    val audioFileUrl: String?,
    val status: String,
    val coverImageUrl: String?,
    val coverVideoUrl: String? = null,
    val createdAt: Instant,
    val emotionMode: String? = null
)
