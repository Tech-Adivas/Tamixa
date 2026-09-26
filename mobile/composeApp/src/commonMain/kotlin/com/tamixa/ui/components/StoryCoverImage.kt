package com.tamixa.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.story_card_default
import com.tamixa.domain.Story
import com.tamixa.network.ApiConfig
import com.tamixa.platform.CoverVideoSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import com.tamixa.ui.strings.Strings
import org.jetbrains.compose.resources.painterResource

/**
 * HD cover for story cards — **prefers** animated [Story.coverVideoUrl] when present (GIF or MP4),
 * with [Story.coverImageUrl] as poster/fallback. GIF is shown as the primary layer (not stacked on
 * a duplicate full-size static decode). MP4 uses [CoverVideoSurface] over the static poster.
 * Resolves relative paths (e.g. /api/v1/covers/...) via apiBaseUrl when provided.
 */
object DefaultCoverImages {
    /** Picsum seeds for consistent HD placeholder per theme */
    private fun themeSeed(theme: String): Int = theme.hashCode().let { if (it < 0) -it else it }

    /** Returns a stable HD placeholder URL for the given theme (16:10 aspect for story covers) */
    fun placeholderUrl(theme: String, width: Int = 640, height: Int = 400): String =
        "https://picsum.photos/seed/${themeSeed(theme)}/$width/$height"
}

/**
 * @param coverRefreshKey Optional cache-bust key (e.g. timestamp) to append as ?t= when cover
 *   was regenerated and URL path may be unchanged. Pass when cover was just generated.
 */
@Composable
fun StoryCoverImage(
    story: Story,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    apiBaseUrl: String? = null,
    coverRefreshKey: Long? = null
) {
    val reduceMotion = platformIsReduceMotionEnabled()
    val rawUrl = story.coverImageUrl?.takeIf { it.isNotBlank() }
    
    // If no cover image URL, show default placeholder immediately
    if (rawUrl == null) {
        DefaultStoryCoverPlaceholder(modifier = modifier)
        return
    }

    val imageUrl = remember(story.coverImageUrl, apiBaseUrl, coverRefreshKey) {
        val baseImageUrl = when {
            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
            rawUrl.startsWith("/") && apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawUrl)
            apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawUrl)
            else -> null
        }
        baseImageUrl?.let { url -> if (coverRefreshKey != null) "$url?t=$coverRefreshKey" else url }
    }

    val videoUrl = remember(story.coverVideoUrl, apiBaseUrl, coverRefreshKey) {
        val rawVideoUrl = story.coverVideoUrl?.takeIf { it.isNotBlank() }
        val baseVideoUrl = when {
            rawVideoUrl == null -> null
            rawVideoUrl.startsWith("http://") || rawVideoUrl.startsWith("https://") -> rawVideoUrl
            apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawVideoUrl)
            else -> null
        }
        baseVideoUrl?.let { url -> if (coverRefreshKey != null) "$url?t=$coverRefreshKey" else url }
    }

    val (bgStart, bgEnd) = themeGradient(story.theme)
    val gifCoverUrl = videoUrl?.takeIf { it.lowercase().contains(".gif") }
    val coverContentDescription = Strings.storyCoverContentDescription(story.title, story.theme)
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgStart, bgEnd)
                )
            )
    ) {
        when {
            !reduceMotion && gifCoverUrl != null -> {
                coil3.compose.SubcomposeAsyncImage(
                    model = gifCoverUrl,
                    contentDescription = coverContentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = {
                        if (imageUrl != null) {
                            coil3.compose.SubcomposeAsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = contentScale,
                                loading = { StoryCoverLoadingSlot(story.theme, Modifier.fillMaxSize()) },
                                error = { DefaultStoryCoverPlaceholder(modifier = Modifier.fillMaxSize()) }
                            )
                        } else {
                            StoryCoverLoadingSlot(story.theme, Modifier.fillMaxSize())
                        }
                    },
                    error = {
                        if (imageUrl != null) {
                            coil3.compose.SubcomposeAsyncImage(
                                model = imageUrl,
                                contentDescription = coverContentDescription,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = contentScale,
                                loading = { StoryCoverLoadingSlot(story.theme, Modifier.fillMaxSize()) },
                                error = { DefaultStoryCoverPlaceholder(modifier = Modifier.fillMaxSize()) }
                            )
                        } else {
                            DefaultStoryCoverPlaceholder(modifier = Modifier.fillMaxSize())
                        }
                    }
                )
            }
            else -> {
                if (imageUrl != null) {
                    coil3.compose.SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = coverContentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale,
                        error = { 
                            DefaultStoryCoverPlaceholder(modifier = Modifier.fillMaxSize()) 
                        }
                    )
                } else {
                    DefaultStoryCoverPlaceholder(modifier = Modifier.fillMaxSize())
                }
                if (videoUrl != null && !reduceMotion) {
                    CoverVideoSurface(
                        videoUrl = videoUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Warm cache for [story] cover before opening the player. */
fun prefetchStoryCoverForStory(story: Story, apiBaseUrl: String?) {
    prefetchStoryCover(resolvedCoverImageUrl(story, apiBaseUrl))
}

private fun resolvedCoverImageUrl(story: Story, apiBaseUrl: String?): String? {
    val rawUrl = story.coverImageUrl?.takeIf { it.isNotBlank() } ?: return null
    val baseImageUrl = when {
        rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
        rawUrl.startsWith("/") && apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawUrl)
        apiBaseUrl != null -> ApiConfig.resolveCoverUrl(apiBaseUrl, rawUrl)
        else -> null
    }
    return baseImageUrl
}

@Composable
private fun StoryCoverLoadingSlot(theme: String, modifier: Modifier = Modifier) {
    if (platformIsReduceMotionEnabled()) {
        val (startColor, endColor) = themeGradient(theme)
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(colors = listOf(startColor, endColor))
            ),
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
    } else {
        StoryCoverPlaceholder(theme, modifier)
    }
}

/**
 * Default story card illustration: Tamil Nadu boy with lantern in magical forest.
 * Used when cover image is missing, loading, or failed.
 */
@Composable
fun DefaultStoryCoverPlaceholder(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.story_card_default),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

/**
 * Themed gradient placeholder when image is loading or for previews.
 * @deprecated Use DefaultStoryCoverPlaceholder for story cards. Kept for backward compatibility.
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
        lower.contains("fox") || lower.contains("monkey") || lower.contains("animal") || lower.contains("animals") ->
            Color(0xFFE8A54B) to Color(0xFFC17817)
        lower.contains("ant") || lower.contains("grasshopper") ->
            Color(0xFF7CB342) to Color(0xFF558B2F)
        lower.contains("lion") || lower.contains("rabbit") ->
            Color(0xFFF9A825) to Color(0xFFEF6C00)
        lower.contains("ocean") || lower.contains("sea") || lower.contains("nature") ->
            Color(0xFF4FC3F7) to Color(0xFF0288D1)
        lower.contains("night") || lower.contains("bed") || lower.contains("moon") ->
            Color(0xFF5C6BC0) to Color(0xFF3949AB)
        lower.contains("space") || lower.contains("star") || lower.contains("fantasy") ->
            Color(0xFF7E57C2) to Color(0xFF4527A0)
        lower.contains("tenali") || lower.contains("king") || lower.contains("thieves") ->
            Color(0xFFAB47BC) to Color(0xFF6A1B9A)
        lower.contains("friendship") || lower.contains("family") ->
            Color(0xFFC4625A) to Color(0xFFA84D45)
        lower.contains("adventure") || lower.contains("bravery") ->
            Color(0xFF2D5A5A) to Color(0xFF1E4545)
        lower.contains("village") || lower.contains("moral") ->
            Color(0xFF7ED9C4) to Color(0xFF5FB8A0)
        lower.contains("funny") ->
            Color(0xFFE8A54B) to Color(0xFFC4625A)
        else -> Color(0xFF2A2735) to Color(0xFF18151F)
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
