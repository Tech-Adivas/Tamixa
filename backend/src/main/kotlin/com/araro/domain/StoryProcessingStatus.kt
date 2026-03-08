package com.araro.domain

import java.time.Instant

data class StoryProcessingStatus(
    val id: Long,
    val masterStoryId: Long,
    val language: String,
    val stage: ProcessingStage,
    val retryCount: Int,
    val lastError: String?,
    val updatedAt: Instant
)
