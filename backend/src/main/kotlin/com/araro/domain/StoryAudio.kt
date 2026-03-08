package com.araro.domain

import java.time.Instant

data class StoryAudio(
    val id: Long,
    val masterStoryId: Long,
    val language: String,
    val audioFileUrl: String,
    val createdAt: Instant
)
