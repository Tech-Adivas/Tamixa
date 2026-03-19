package com.tamixa.domain.narration

import java.time.Instant

/**
 * Neural TTS audio output for a translated story narration.
 * Path: stories/{storyId}/{language}/v1.mp3
 */
data class StoryNarrationAudio(
    val id: Long,
    val translationId: Long,
    val voiceProfile: String,
    val audioUrl: String,
    val durationSeconds: Int,
    val status: NarrationAudioStatus,
    val createdAt: Instant,
    val truncationWarning: Boolean = false
)
