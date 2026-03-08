package com.araro.domain

import java.time.Instant

data class CuratedStory(
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
    /** CALM, SOOTHING, ADVENTUROUS. Influences narration ToneMode. */
    val emotionMode: String? = null
)
