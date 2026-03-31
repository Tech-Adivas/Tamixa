package com.tamixa.domain

import java.time.Instant

/** Precomputed scene for AI-generated story playback. */
data class GeneratedStoryScene(
    val id: Long,
    val storyId: Long,
    val language: String,
    val sceneIndex: Int,
    val backgroundHint: String? = null,
    val illustrationImagePath: String? = null,
    val createdAt: Instant,
    val segments: List<GeneratedStorySegment> = emptyList()
)

data class GeneratedStorySegment(
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
