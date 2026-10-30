package com.tamixa.network

import kotlinx.serialization.Serializable

/**
 * Timed picture for the player. Story slideshow shots also carry framing: [camera] (wide | medium | close_up),
 * [focus] (region of the subject: left, right, top_left, ...; null = whole picture) and [movement]
 * (none, slow_zoom_in, slow_zoom_out, pan_left, pan_right, pan_up, pan_down).
 */
@Serializable
data class NarrativeSceneVisual(
    val sceneIndex: Int,
    val startProgress: Float,
    val illustrationUrl: String? = null,
    val backgroundHint: String? = null,
    val camera: String? = null,
    val focus: String? = null,
    val movement: String? = null,
)
