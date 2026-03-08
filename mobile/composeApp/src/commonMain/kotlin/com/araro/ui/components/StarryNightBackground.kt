package com.araro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.araro.ui.theme.AraroGradients
import kotlin.random.Random

/**
 * Ultra HD starry night background with soft gradient, golden stars, and ethereal glow.
 * Premium audio-story app aesthetic: warm, immersive, professional.
 */
@Composable
fun StarryNightBackground(
    modifier: Modifier = Modifier,
    showStars: Boolean = true,
    showClouds: Boolean = true
) {
    val gradient = AraroGradients.nightSkyBackground
    val starPositions = rememberStarPositions()
    val cloudPositions = rememberCloudPositions()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = gradient,
                    startY = 0f,
                    endY = size.height
                )
            )
            // Subtle golden glow near top (warm accent for premium feel)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D2F6E).copy(alpha = 0f),
                        Color(0xFFF6C453).copy(alpha = 0.03f),
                        Color(0xFF2D2F6E).copy(alpha = 0f)
                    ),
                    startY = 0f,
                    endY = size.height * 0.4f
                )
            )
        }

        if (showStars) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = minOf(size.width, size.height)
                starPositions.forEach { star ->
                    val color = if (star.isYellow) {
                        Color(0xFFF6C453).copy(alpha = star.alpha * 0.9f)
                    } else {
                        Color(0xFFE8E0F0).copy(alpha = star.alpha * 0.95f)
                    }
                    val radius = star.radius * minDim * 0.014f
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(star.x * size.width, star.y * size.height)
                    )
                    // Soft glow for larger stars
                    if (star.radius > 1.2f) {
                        drawCircle(
                            color = color.copy(alpha = star.alpha * 0.25f),
                            radius = radius * 2f,
                            center = Offset(star.x * size.width, star.y * size.height)
                        )
                    }
                }
            }
        }

        if (showClouds) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                cloudPositions.forEach { cloud ->
                    drawCloud(
                        center = Offset(cloud.x * size.width, cloud.y * size.height),
                        widthRatio = cloud.widthRatio,
                        heightRatio = cloud.heightRatio,
                        alpha = cloud.alpha * 0.7f
                    )
                }
            }
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
