package com.tamixa.api.admin.dto

/**
 * Per-story AI usage for admin story-wise metrics.
 */
data class StoryAiUsageDto(
    val storyId: Long,
    val title: String?,
    val totalTokens: Long,
    val avatarVideoCount: Long,
    val costInr: Double
)
