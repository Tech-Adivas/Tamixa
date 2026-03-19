package com.tamixa.domain

import java.time.Instant

/**
 * Family recorded voice: parent uploads MP3 per story.
 * Stored in private path families/{parentId}/stories/{storyId}/{language}.mp3
 */
data class StoryFamilyVoice(
    val id: Long,
    val storyId: Long,
    val parentId: Long,
    val language: String,
    val storagePath: String,
    val fileSizeBytes: Long,
    val createdAt: Instant
)
