package com.araro.domain

import java.time.Instant

/**
 * Tracks soundscape usage per parent/story.
 */
data class SoundscapeUsage(
    val id: Long,
    val parentId: Long,
    val storyId: Long?,
    val soundscapeId: Long,
    val usageCount: Int,
    val lastUsedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
