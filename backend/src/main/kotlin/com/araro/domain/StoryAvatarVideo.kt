package com.araro.domain

import java.time.Instant

/**
 * Cached avatar video (talking-head) for a story.
 * Generated via Replicate SadTalker: avatar image + narration audio → video.
 */
data class StoryAvatarVideo(
    val id: Long,
    val storyId: Long,
    val storySource: String,
    val parentId: Long,
    val language: String,
    val voiceProfile: String,
    val storagePath: String?,
    val status: AvatarVideoStatus,
    val replicatePredictionId: String?,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class AvatarVideoStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
