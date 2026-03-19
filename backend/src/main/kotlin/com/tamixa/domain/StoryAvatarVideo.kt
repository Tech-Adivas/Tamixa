package com.tamixa.domain

import java.time.Instant

/**
 * Cached avatar video (talking-head) for a specific story.
 * One record per (storyId, storySource, parentId, language, voiceProfile). Avatar generation
 * is always story-specific: each story has its own video from that story's narration audio.
 * Generated via HeyGen/Replicate/D-ID/Gooey: avatar image + this story's audio → video.
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
