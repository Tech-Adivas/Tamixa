package com.tamixa.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.composeapp.generated.resources.themeBackgroundDrawable
import com.tamixa.ui.theme.TamixaGradients
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource

private val FallbackNightBg = Color(0xFF1A1812)

/**
 * Ultra HD starry night background with soft gradient, golden stars, and ethereal glow.
 * Uses a solid fallback so content is always visible even if gradient doesn't render.
 * @param animateStars when true, stars drift slowly for a calm bedtime feel (e.g. on login).
 */
@Composable
fun StarryNightBackground(
    modifier: Modifier = Modifier,
    showStars: Boolean = true,
    showClouds: Boolean = true,
    animateStars: Boolean = false
) {
    val gradient = TamixaGradients.nightSkyBackground
    val starPositions = rememberStarPositions()
    val cloudPositions = rememberCloudPositions()
    val starPhase = if (animateStars) {
        val infinite = rememberInfiniteTransition(label = "stars")
        val phase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(12_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "starPhase"
        )
        phase
    } else 0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FallbackNightBg)
    ) {
        val screenWidthDp = maxWidth.value.toInt()
        val screenHeightDp = maxHeight.value.toInt()
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = gradient,
                    startY = 0f,
                    endY = size.height
                )
            )
            // Storybook Dusk: subtle terracotta glow near top (warm, distinctive)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1812).copy(alpha = 0f),
                        Color(0xFFC4625A).copy(alpha = 0.04f),
                        Color(0xFF1A1812).copy(alpha = 0f)
                    ),
                    startY = 0f,
                    endY = size.height * 0.4f
                )
            )
        }

        if (showStars) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = minOf(size.width, size.height)
                val driftAmplitude = 0.008f
                starPositions.forEachIndexed { index, star ->
                    val drift = (star.x * 2f + star.y + index * 0.1f) * (2 * kotlin.math.PI).toFloat()
                    val dx = (kotlin.math.sin(starPhase * 2 * kotlin.math.PI + drift) * driftAmplitude * size.width).toFloat()
                    val dy = (kotlin.math.cos(starPhase * 2 * kotlin.math.PI * 0.7 + drift * 1.3) * driftAmplitude * size.height).toFloat()
                    val center = Offset(
                        star.x * size.width + dx,
                        star.y * size.height + dy
                    )
                    val twinkle = if (animateStars) {
                        0.5f + 0.5f * kotlin.math.sin(starPhase * 4 * kotlin.math.PI + index).toFloat()
                    } else 1f
                    val color = if (star.isYellow) {
                        Color(0xFFE8DCC8).copy(alpha = star.alpha * 0.8f * twinkle)
                    } else {
                        Color(0xFFC4625A).copy(alpha = star.alpha * 0.35f * twinkle)
                    }
                    val radius = star.radius * minDim * 0.012f
                    drawCircle(color = color, radius = radius, center = center)
                    if (star.radius > 1.2f) {
                        drawCircle(
                            color = color.copy(alpha = star.alpha * 0.18f * twinkle),
                            radius = radius * 2f,
                            center = center
                        )
                    }
                }
            }
        }

        if (showClouds) {
            val cloudSizeDp = (screenWidthDp * 0.28f).toInt().coerceIn(70, 120)
            Box(modifier = Modifier.fillMaxSize()) {
                cloudPositions.forEach { cloud ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (cloud.x * screenWidthDp - cloudSizeDp / 2).dp,
                                y = (cloud.y * screenHeightDp - cloudSizeDp / 2).dp
                            )
                            .size(cloudSizeDp.dp)
                            .graphicsLayer { alpha = cloud.alpha * 0.65f }
                    ) {
                        SoftCloud(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Soft floating lantern for bedtime / welcome screens. Gentle vertical and horizontal drift with glow.
 */
@Composable
fun FloatingLantern(
    modifier: Modifier = Modifier,
    lanternSize: Dp = 72.dp
) {
    val infinite = rememberInfiniteTransition(label = "lantern")
    val driftX by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lanternX"
    )
    val driftY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lanternY"
    )
    val offsetXDp = (kotlin.math.sin(driftX * 2 * kotlin.math.PI).toFloat() * 12).toInt().dp
    val offsetYDp = (kotlin.math.cos(driftY * 2 * kotlin.math.PI).toFloat() * 8).toInt().dp

    Box(
        modifier = modifier
            .size(lanternSize)
            .offset(offsetXDp, offsetYDp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) * 0.38f
            val glowRadius = r * 2.2f
            val glowCenter = Offset(cx, cy)
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF5E6D3).copy(alpha = 0.95f),
                    Color(0xFFC4625A).copy(alpha = 0.5f),
                    Color(0xFFC4625A).copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = glowCenter,
                radius = glowRadius
            )
            drawCircle(glowBrush, glowRadius, glowCenter)
            drawCircle(
                color = Color(0xFFFDF8F5),
                center = Offset(cx, cy),
                radius = r
            )
            drawCircle(
                color = Color(0xFFC4625A).copy(alpha = 0.4f),
                center = Offset(cx, cy),
                radius = r * 0.7f
            )
        }
    }
}

@Composable
private fun rememberStarPositions(): List<StarInfo> {
    return androidx.compose.runtime.remember {
        val random = Random(42)
        (1..55).map {
            StarInfo(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = random.nextFloat() * 1.5f + 0.5f,
                alpha = random.nextFloat() * 0.5f + 0.3f,
                isYellow = random.nextBoolean()
            )
        }
    }
}

private data class StarInfo(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val isYellow: Boolean
)

@Composable
private fun rememberCloudPositions(): List<CloudInfo> {
    return androidx.compose.runtime.remember {
        listOf(
            CloudInfo(0.15f, 0.08f, 0.25f, 0.06f, 0.06f),
            CloudInfo(0.75f, 0.12f, 0.2f, 0.05f, 0.05f),
            CloudInfo(0.5f, 0.35f, 0.3f, 0.07f, 0.05f),
            CloudInfo(0.25f, 0.7f, 0.22f, 0.05f, 0.06f),
            CloudInfo(0.85f, 0.6f, 0.18f, 0.04f, 0.05f),
            CloudInfo(0.1f, 0.45f, 0.15f, 0.04f, 0.04f),
            CloudInfo(0.9f, 0.85f, 0.2f, 0.05f, 0.05f)
        )
    }
}

/** Soft cloud shape drawn with overlapping circles – replaces ic_cloud_soft_wispy image. */
@Composable
internal fun SoftCloud(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val w = size.width * 0.9f
        val h = size.height * 0.9f
        val color = Color.White.copy(alpha = 0.5f)
        drawCircle(color = color, radius = w * 0.35f, center = center)
        drawCircle(color = color, radius = w * 0.25f, center = center + Offset(-w * 0.3f, -h * 0.2f))
        drawCircle(color = color, radius = w * 0.28f, center = center + Offset(w * 0.3f, -h * 0.15f))
        drawCircle(color = color, radius = w * 0.22f, center = center + Offset(-w * 0.15f, h * 0.1f))
        drawCircle(color = color, radius = w * 0.2f, center = center + Offset(w * 0.2f, h * 0.1f))
    }
}

private data class CloudInfo(
    val x: Float,
    val y: Float,
    val widthRatio: Float,
    val heightRatio: Float,
    val alpha: Float
)

private fun DrawScope.drawCloud(
    center: Offset,
    widthRatio: Float,
    heightRatio: Float,
    alpha: Float
) {
    val color = Color.White.copy(alpha = alpha)
    val w = size.width * widthRatio
    val h = size.height * heightRatio
    drawCircle(color = color, radius = w * 0.35f, center = center)
    drawCircle(color = color, radius = w * 0.25f, center = center + Offset(-w * 0.3f, -h * 0.2f))
    drawCircle(color = color, radius = w * 0.28f, center = center + Offset(w * 0.3f, -h * 0.15f))
    drawCircle(color = color, radius = w * 0.22f, center = center + Offset(-w * 0.15f, h * 0.1f))
    drawCircle(color = color, radius = w * 0.2f, center = center + Offset(w * 0.2f, h * 0.1f))
}

// Splash-style gradient colors (sky blue → soft purple → deep purple)
private val SplashStyleBlue = Color(0xFF6EC3FF)
private val SplashStyleViolet = Color(0xFF9D8CFF)
private val SplashStyleDeep = Color(0xFF8B7FD4)
private val SplashStyleGlowWarm = Color(0x1FFFFCE8)

/** Cloud position (xFrac, yFrac, alpha) for splash-style background. */
private val SplashStyleCloudPositions = listOf(
    Triple(0.08f, 0.15f, 0.7f),
    Triple(0.78f, 0.22f, 0.55f),
    Triple(0.35f, 0.72f, 0.6f),
    Triple(0.88f, 0.65f, 0.5f),
    Triple(0.15f, 0.52f, 0.45f)
)

/**
 * Full-screen background matching the splash screen: sky blue → purple gradient,
 * soft floating clouds, and optional center glow. Use for consistent look across app.
 */
@Composable
fun SplashStyleBackground(
    modifier: Modifier = Modifier,
    showClouds: Boolean = true,
    showCenterGlow: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SplashStyleBlue, SplashStyleViolet, SplashStyleDeep),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = maxHeight.value.toInt()
        val cloudSizeDp = (widthDp / 4).coerceIn(50, 100)

        if (showClouds) {
            Box(modifier = Modifier.fillMaxSize()) {
                SplashStyleCloudPositions.forEach { (xFrac, yFrac, alpha) ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (xFrac * widthDp - cloudSizeDp / 2).toInt().dp,
                                y = (yFrac * heightDp - cloudSizeDp / 2).toInt().dp
                            )
                            .size(cloudSizeDp.dp)
                            .graphicsLayer { this.alpha = alpha }
                    ) {
                        SoftCloud(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        if (showCenterGlow) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(SplashStyleGlowWarm, Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.35f),
                        radius = size.maxDimension * 0.6f
                    )
                )
            }
        }
    }
}

/**
 * Full-screen background using the Tamixa theme illustration (children by river, South Indian–inspired).
 * Applied across the app for a consistent, colorful, nature-themed look.
 */
@Composable
fun TamixaThemeBackground(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    scrimAlpha: Float = 0.2f
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(themeBackgroundDrawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        // Subtle scrim so text and UI remain readable
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = scrimAlpha),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = scrimAlpha * 0.8f)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
    }
}

/**
 * Standard full-screen background for app screens.
 * Uses StarryNightBackground (old one) across the app for a consistent bedtime/night sky look.
 */
@Composable
fun AppScreenBackground(
    modifier: Modifier = Modifier,
    showStars: Boolean = true,
    showClouds: Boolean = true,
    animateStars: Boolean = false
) {
    StarryNightBackground(
        modifier = modifier,
        showStars = showStars,
        showClouds = showClouds,
        animateStars = animateStars
    )
}
