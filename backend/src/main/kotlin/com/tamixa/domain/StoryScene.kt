package com.tamixa.domain

import java.time.Instant

/**
 * Precomputed scene for playback (e.g. one per story/language).
 */
data class StoryScene(
    val id: Long,
    val translationId: Long,
    val sceneIndex: Int,
    val backgroundHint: String? = null,
    val createdAt: Instant,
    val segments: List<StorySegment> = emptyList()
)

/**
 * Precomputed segment for subtitle sync and timeline.
 * segmentType: NARRATION or DIALOGUE (from emotion tagging).
 */
data class StorySegment(
    val id: Long,
    val sceneId: Long,
    val segmentIndex: Int,
    val speaker: String,
    val text: String,
    val durationMs: Long,
    val audioUrl: String? = null,
    val segmentType: String = "NARRATION",
    val createdAt: Instant
)
