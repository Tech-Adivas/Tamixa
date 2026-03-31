package com.tamixa.api.stream.dto

/**
 * Timeline cue for in-player narrative visuals: crossfade art or tint by [backgroundHint] as playback advances.
 */
data class NarrativeSceneVisualDto(
    val sceneIndex: Int,
    /** Fraction of total story duration (0..1) where this scene becomes active. */
    val startProgress: Float,
    val illustrationUrl: String? = null,
    val backgroundHint: String? = null
)
