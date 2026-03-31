package com.tamixa.network

import kotlinx.serialization.Serializable

@Serializable
data class NarrativeSceneVisual(
    val sceneIndex: Int,
    val startProgress: Float,
    val illustrationUrl: String? = null,
    val backgroundHint: String? = null
)
