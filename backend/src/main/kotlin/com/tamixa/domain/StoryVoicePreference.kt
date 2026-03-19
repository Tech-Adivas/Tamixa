package com.tamixa.domain

import java.time.Instant

/**
 * Parent's preferred voice and playback mode for a specific story.
 * playbackMode: "default" | "my_voice" | "avatar" so we restore Avatar vs My Voice selection.
 */
data class StoryVoicePreference(
    val id: Long,
    val parentId: Long,
    val storyId: Long,
    val storySource: String,
    val voiceProfile: String,
    val playbackMode: String = "default",
    val createdAt: Instant,
    val updatedAt: Instant
)
