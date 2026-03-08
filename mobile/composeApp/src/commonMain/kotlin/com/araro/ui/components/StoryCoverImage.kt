package com.araro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.araro.domain.Story
import com.araro.network.ApiConfig
import com.araro.platform.CoverVideoSurface
import androidx.compose.material3.Text

/**
 * HD cover for story cards - image with optional animated overlay (coverVideoUrl).
 * When coverVideoUrl is a GIF, shows animated GIF on top (Sora image-to-video converted to GIF).
 * When coverVideoUrl is MP4, shows looping muted video. Uses coverImageUrl as poster/fallback.
 * Resolves relative paths (e.g. /api/v1/covers/...) via apiBaseUrl when provided.
 */
object DefaultCoverImages {
    /** Picsum seeds for consistent HD placeholder per theme */
    private fun themeSeed(theme: String): Int = theme.hashCode().let { if (it < 0) -it else it }

    /** Returns a stable HD placeholder URL for the given theme (16:10 aspect for story covers) */
    fun placeholderUrl(theme: String, width: Int = 640, height: Int = 400): String =
        "https://picsum.photos/seed/${themeSeed(theme)}/$width/$height"
}

@Composable
fun StoryCoverImage(
    story: Story,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    apiBaseUrl: String? = null
) {
    val rawUrl = story.coverImageUrl?.takeIf { it.isNotBlank() }
    val imageUrl = when {
        rawUrl != null && (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) -> rawUrl
        rawUrl != null && rawUrl.startsWith("/") && apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawUrl)
        rawUrl != null && apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawUrl)
        else -> null
    }

    val rawVideoUrl = story.coverVideoUrl?.takeIf { it.isNotBlank() }
    val videoUrl = when {
        rawVideoUrl == null -> null
        rawVideoUrl.startsWith("http://") || rawVideoUrl.startsWith("https://") -> rawVideoUrl
        apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawVideoUrl)
        else -> null
    }

    Box(modifier = modifier) {
        if (imageUrl != null) {
            coil3.compose.SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = story.theme,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = { StoryCoverPlaceholder(theme = story.theme, modifier = Modifier.fillMaxSize()) },
                error = { StoryCoverPlaceholder(theme = story.theme, modifier = Modifier.fillMaxSize()) }
            )
        } else {
            StoryCoverPlaceholder(theme = story.theme, modifier = Modifier.fillMaxSize())
        }
        if (videoUrl != null) {
            if (videoUrl.lowercase().contains(".gif")) {
                coil3.compose.SubcomposeAsyncImage(
                    model = videoUrl,
                    contentDescription = story.theme,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = { StoryCoverPlaceholder(theme = story.theme, modifier = Modifier.fillMaxSize()) },
                    error = { }
                )
            } else {
                CoverVideoSurface(
                    videoUrl = videoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Themed gradient placeholder when image is loading or for previews.
 * Kid-friendly gradient with subtle shimmer animation based on story theme.
 */
@Composable
fun StoryCoverPlaceholder(
    theme: String,
    modifier: Modifier = Modifier
) {
    val (startColor, endColor) = themeGradient(theme)
    val infiniteTransition = rememberInfiniteTransition(label = "placeholderShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    val shimmerColor = Color.White.copy(alpha = shimmerAlpha)
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(startColor, endColor)
                )
            )
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            shimmerColor,
                            Color.Transparent
                        )
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = themeEmoji(theme),
            fontSize = 40.sp,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun themeGradient(theme: String): Pair<Color, Color> {
    val lower = theme.lowercase()
    return when {
        lower.contains("fox") || lower.contains("monkey") || lower.contains("animal") ->
            Color(0xFFE8A54B) to Color(0xFFC17817)
        lower.contains("ant") || lower.contains("grasshopper") ->
            Color(0xFF7CB342) to Color(0xFF558B2F)
        lower.contains("lion") || lower.contains("rabbit") ->
            Color(0xFFF9A825) to Color(0xFFEF6C00)
        lower.contains("ocean") || lower.contains("sea") ->
            Color(0xFF4FC3F7) to Color(0xFF0288D1)
        lower.contains("night") || lower.contains("bed") || lower.contains("moon") ->
            Color(0xFF5C6BC0) to Color(0xFF3949AB)
        lower.contains("space") || lower.contains("star") ->
            Color(0xFF7E57C2) to Color(0xFF4527A0)
        lower.contains("tenali") || lower.contains("king") || lower.contains("thieves") ->
            Color(0xFFAB47BC) to Color(0xFF6A1B9A)
        else -> Color(0xFFF5EDE4) to Color(0xFFE8DCC8)
    }
}

private fun themeEmoji(theme: String): String {
    val lower = theme.lowercase()
    return when {
        lower.contains("fox") -> "🦊"
        lower.contains("monkey") || lower.contains("crocodile") -> "🐵"
        lower.contains("ant") || lower.contains("grasshopper") -> "🦗"
        lower.contains("lion") || lower.contains("rabbit") -> "🦁"
        lower.contains("ocean") || lower.contains("sea") -> "🌊"
        lower.contains("night") || lower.contains("bed") || lower.contains("moon") -> "🌙"
        lower.contains("space") || lower.contains("star") -> "⭐"
        lower.contains("tenali") || lower.contains("king") -> "👑"
        lower.contains("donkey") || lower.contains("கழுதை") -> "🫏"
        else -> "📖"
    }
}
