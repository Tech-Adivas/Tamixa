package com.tamixa.api.stream.dto

/**
 * Timeline cue for in-player narrative visuals: crossfade art or tint by [backgroundHint] as playback advances.
 * Story slideshow shots also say how to frame the picture: [camera] (wide | medium | close_up), [focus] (region of
 * the subject to move to, e.g. left; null = whole picture) and [movement] (slow_zoom_in, pan_left, ...).
 */
data class NarrativeSceneVisualDto(
    val sceneIndex: Int,
    /** Fraction of total story duration (0..1) where this scene becomes active. */
    val startProgress: Float,
    val illustrationUrl: String? = null,
    val backgroundHint: String? = null,
    val camera: String? = null,
    val focus: String? = null,
    val movement: String? = null,
)
