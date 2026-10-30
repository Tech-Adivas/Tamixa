package com.tamixa.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    val resolve = { raw: String? ->
        raw?.trim()?.takeIf { it.isNotEmpty() }?.let {
            when {
                // Same handling as covers: absolute backend URLs (e.g. localhost) are normalized to the app's API host.
                apiBaseUrl != null -> com.tamixa.network.ApiConfig.resolveCoverUrl(apiBaseUrl, it)
                it.startsWith("http://") || it.startsWith("https://") -> it
                else -> null
            }
        }
    }
    val illustration = resolve(active.illustrationUrl)
    PreloadNextIllustration(scenes, active, resolve)
    Box(modifier = modifier.fillMaxSize()) {
        if (!illustration.isNullOrBlank()) {
            Crossfade(
                targetState = illustration,
                // Slow, gentle scene change for bedtime listening.
                animationSpec = tween(if (reduceMotion) 300 else 1200),
                modifier = Modifier.fillMaxSize(),
                label = "narrativeIllustration"
            ) { url ->
                // Shots on the same picture stay in this composable, so the camera glides between their framings.
                CameraIllustration(url, shot = active.takeIf { resolve(it.illustrationUrl) == url }, reduceMotion = reduceMotion)
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

/** Starts downloading the next different picture early so it is ready when its shot begins. */
@Composable
private fun PreloadNextIllustration(
    scenes: List<NarrativeSceneVisual>,
    active: NarrativeSceneVisual,
    resolve: (String?) -> String?,
) {
    val current = resolve(active.illustrationUrl)
    val next = scenes.sortedBy { it.startProgress }
        .filter { it.startProgress > active.startProgress }
        .firstNotNullOfOrNull { resolve(it.illustrationUrl)?.takeIf { url -> url != current } }
        ?: return
    val context = coil3.compose.LocalPlatformContext.current
    LaunchedEffect(next) {
        coil3.SingletonImageLoader.get(context).enqueue(coil3.request.ImageRequest.Builder(context).data(next).build())
    }
}

/**
 * A still picture filmed like a camera: the shot's framing (wide / medium / close-up on the subject's region) plus a
 * slow drift (zoom or pan) while the shot lasts. Scenes without framing (older story scenes) get a gentle zoom with an
 * alternating pan, so a picture never feels frozen. The picture is never moved far enough to show its edge.
 */
@Composable
private fun CameraIllustration(url: String, shot: NarrativeSceneVisual?, reduceMotion: Boolean) {
    val framing = cameraFraming(shot, url)
    val glide = tween<Float>(durationMillis = if (reduceMotion) 0 else CAMERA_GLIDE_MS)
    val scale by animateFloatAsState(if (reduceMotion) 1f else framing.scale, glide, label = "cameraScale")
    val centerX by animateFloatAsState(if (reduceMotion) 0.5f else framing.centerX, glide, label = "cameraX")
    val centerY by animateFloatAsState(if (reduceMotion) 0.5f else framing.centerY, glide, label = "cameraY")
    val drift = remember(url, shot?.sceneIndex) { Animatable(0f) }
    LaunchedEffect(url, shot?.sceneIndex, reduceMotion) {
        if (!reduceMotion) drift.animateTo(1f, tween(durationMillis = CAMERA_DRIFT_MS, easing = LinearEasing))
    }
    coil3.compose.SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val t = drift.value
                val s = (scale * (1f + framing.zoomDrift * t)).coerceAtLeast(1f)
                val cx = centerX + framing.panDriftX * t
                val cy = centerY + framing.panDriftY * t
                scaleX = s
                scaleY = s
                // Bring (cx, cy) of the picture to the middle, but only as far as the zoom leaves spare picture.
                val maxX = (s - 1f) / 2f * size.width
                val maxY = (s - 1f) / 2f * size.height
                translationX = ((0.5f - cx) * size.width * s).coerceIn(-maxX, maxX)
                translationY = ((0.5f - cy) * size.height * s).coerceIn(-maxY, maxY)
            },
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize()) },
        error = { Box(Modifier.fillMaxSize()) }
    )
}

private data class CameraFraming(
    val scale: Float,
    val centerX: Float,
    val centerY: Float,
    val zoomDrift: Float = 0f,
    val panDriftX: Float = 0f,
    val panDriftY: Float = 0f,
)

private fun cameraFraming(shot: NarrativeSceneVisual?, url: String): CameraFraming {
    if (shot?.camera == null) {
        // Older story scenes: gentle zoom with a pan that alternates per picture.
        val panLeft = (url.hashCode() and 1) == 0
        return CameraFraming(1f, 0.5f, 0.5f, zoomDrift = 0.12f, panDriftX = if (panLeft) 0.06f else -0.06f)
    }
    val scale = when (shot.camera) {
        "close_up" -> 1.75f
        "medium" -> 1.35f
        else -> 1f
    }
    val (cx, cy) = when (shot.focus) {
        "left" -> 0.28f to 0.5f
        "right" -> 0.72f to 0.5f
        "top" -> 0.5f to 0.3f
        "bottom" -> 0.5f to 0.7f
        "top_left" -> 0.3f to 0.3f
        "top_right" -> 0.7f to 0.3f
        "bottom_left" -> 0.3f to 0.7f
        "bottom_right" -> 0.7f to 0.7f
        else -> 0.5f to 0.5f
    }
    return when (shot.movement) {
        "slow_zoom_in" -> CameraFraming(scale, cx, cy, zoomDrift = 0.12f)
        "slow_zoom_out" -> CameraFraming(scale * 1.12f, cx, cy, zoomDrift = -0.1f)
        // A pan needs spare picture: start slightly zoomed and move the view across.
        "pan_left" -> CameraFraming(maxOf(scale, 1.15f), cx + 0.08f, cy, panDriftX = -0.16f)
        "pan_right" -> CameraFraming(maxOf(scale, 1.15f), cx - 0.08f, cy, panDriftX = 0.16f)
        "pan_up" -> CameraFraming(maxOf(scale, 1.15f), cx, cy + 0.08f, panDriftY = -0.16f)
        "pan_down" -> CameraFraming(maxOf(scale, 1.15f), cx, cy - 0.08f, panDriftY = 0.16f)
        else -> CameraFraming(scale, cx, cy, zoomDrift = 0.03f)
    }
}

/** Time to glide from one shot's framing to the next on the same picture. */
private const val CAMERA_GLIDE_MS = 1_600
/** A shot's slow drift (zoom/pan) spreads over about the length of a shot. */
private const val CAMERA_DRIFT_MS = 25_000

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
