package com.tamixa.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.tamixa.network.NarrativeSceneVisual

fun narrativeLayerWorthShowing(scenes: List<NarrativeSceneVisual>): Boolean {
    if (scenes.isEmpty()) return false
    if (scenes.size > 1) return true
    val s = scenes.first()
    return !s.illustrationUrl.isNullOrBlank() || !s.backgroundHint.isNullOrBlank()
}

fun activeNarrativeScene(progress: Float, scenes: List<NarrativeSceneVisual>): NarrativeSceneVisual? {
    if (scenes.isEmpty()) return null
    val ordered = scenes.sortedWith(compareBy({ it.startProgress }, { it.sceneIndex }))
    var active = ordered.first()
    for (s in ordered) {
        if (progress + 0.0001f >= s.startProgress) active = s
        else break
    }
    return active
}

@Composable
fun NarrativeSceneOverlay(
    progress: Float,
    scenes: List<NarrativeSceneVisual>,
    apiBaseUrl: String?,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (!narrativeLayerWorthShowing(scenes)) return
    val active = activeNarrativeScene(progress, scenes) ?: return
    val illustration = active.illustrationUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
        when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("/") && apiBaseUrl != null -> com.tamixa.network.ApiConfig.resolveCoverUrl(apiBaseUrl, raw)
            apiBaseUrl != null -> com.tamixa.network.ApiConfig.resolveCoverUrl(apiBaseUrl, raw)
            else -> null
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        if (!illustration.isNullOrBlank()) {
            if (reduceMotion) {
                coil3.compose.SubcomposeAsyncImage(
                    model = illustration,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize()) },
                    error = { Box(Modifier.fillMaxSize()) }
                )
            } else {
                Crossfade(
                    targetState = illustration,
                    animationSpec = tween(480),
                    modifier = Modifier.fillMaxSize(),
                    label = "narrativeIllustration"
                ) { url ->
                    coil3.compose.SubcomposeAsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { Box(Modifier.fillMaxSize()) },
                        error = { Box(Modifier.fillMaxSize()) }
                    )
                }
            }
        }
        val wash = backgroundHintWash(active.backgroundHint)
        if (wash != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(wash)
            )
        }
    }
}

private fun backgroundHintWash(hint: String?): Brush? {
    val h = hint?.lowercase()?.trim() ?: return null
    val (a, b) = when {
        "forest" in h || "adventure" in h -> Color(0xFF2E4A3A) to Color(0xFF1A2E24)
        "garden" in h || "nature" in h -> Color(0xFF3D5C3A) to Color(0xFF2A4028)
        "village" in h || "home" in h || "cozy" in h -> Color(0xFF5C4A3A) to Color(0xFF3D3228)
        "beach" in h || "sea" in h || "ocean" in h -> Color(0xFF2A4A5C) to Color(0xFF1A3344)
        "night" in h || "star" in h || "moon" in h -> Color(0xFF2A2A48) to Color(0xFF141428)
        "meadow" in h || "animal" in h -> Color(0xFF4A5A38) to Color(0xFF2F3A24)
        else -> return null
    }
    return Brush.verticalGradient(
        colors = listOf(
            a.copy(alpha = 0.38f),
            Color.Transparent,
            b.copy(alpha = 0.32f)
        )
    )
}
