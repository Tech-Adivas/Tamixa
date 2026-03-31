package com.tamixa.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Story card: vertical layout — cover on top, content below. Professional list style.
 */
@Composable
fun StoryCard(
    story: Story,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailContent: @Composable (() -> Unit)? = null,
    visible: Boolean = true,
    animationDelayMillis: Int = 0,
    apiBaseUrl: String? = null,
    /** Progress 0f-1f for Continue Listening; null = no progress bar */
    progress: Float? = null,
    /** When non-null, shows heart icon. Boolean = currently favorite. */
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    /** When false, hides the play button (e.g. dashboard for cleaner look). */
    showPlayButton: Boolean = true
) {
    val defaultThumbnail: @Composable () -> Unit = {
        StoryCoverImage(story = story, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, apiBaseUrl = apiBaseUrl)
    }
    val effectiveThumbnail = thumbnailContent ?: defaultThumbnail
    val entranceAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(280, delayMillis = animationDelayMillis), label = "entranceAlpha"
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.97f,
        animationSpec = tween(280, delayMillis = animationDelayMillis), label = "entranceScale"
    )
    val isGenerating = story.status == StoryStatus.GENERATING
    val shimmerAlpha = if (isGenerating) {
        val infinite = rememberInfiniteTransition(label = "shimmer")
        infinite.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        ).value
    } else 0f

    val borderModifier = if (isGenerating) {
        Modifier.border(
            width = 2.dp,
            color = TamixaColors.goldAccent.copy(alpha = shimmerAlpha),
            shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius)
        )
    } else Modifier

    val colorScheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(TamixaDesignTokens.cardRadius)
    Card(
        onClick = { if (story.status == StoryStatus.READY) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .shadow(
                elevation = TamixaDesignTokens.listCardShadowElevation,
                shape = cardShape,
                ambientColor = TamixaDesignTokens.listCardShadowAmbient,
                spotColor = TamixaDesignTokens.listCardShadowSpot
            )
            .scale(entranceScale)
            .alpha(entranceAlpha),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(
                        topStart = TamixaDesignTokens.cardRadius,
                        bottomStart = TamixaDesignTokens.cardRadius
                    ))
                    .background(colorScheme.surfaceContainerHighest)
            ) {
                effectiveThumbnail()
                if (onFavoriteClick != null && story.status == StoryStatus.READY) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(TamixaDesignTokens.minTouchTargetSize)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                            contentDescription = Strings.favorites(),
                            tint = if (isFavorite) TamixaColors.goldAccent else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(TamixaDesignTokens.smallSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.title?.takeIf { it.isNotBlank() } ?: story.theme,
                        style = MaterialTheme.typography.titleSmall.copy(
                            letterSpacing = (-0.1).sp
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (story.status != StoryStatus.READY) {
                        Spacer(Modifier.height(4.dp))
                        StatusChip(status = story.status)
                    }
                }
                if (story.status == StoryStatus.READY && showPlayButton) {
                    Surface(
                        shape = CircleShape,
                        color = TamixaColors.goldAccent,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = Strings.play(),
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                }
            }
        }
        if (progress != null && progress in 0f..1f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TamixaDesignTokens.smallSpacing, vertical = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50)),
                color = TamixaColors.goldAccent
            )
        }
        }
    }
}

@Composable
fun StoryThumbnailPlaceholder(theme: String) {
    val emoji = when (theme.take(3).hashCode() % 5) {
        0 -> "🌙"
        1 -> "📖"
        2 -> "🦊"
        3 -> "🐘"
        else -> "⭐"
    }
    Text(
        text = emoji,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 32.sp
        )
    )
}

@Composable
fun StatusChip(status: StoryStatus) {
    val (label, color) = when (status) {
        StoryStatus.READY -> "READY" to MaterialTheme.colorScheme.primary
        StoryStatus.GENERATING -> "GENERATING" to MaterialTheme.colorScheme.tertiary
        StoryStatus.FAILED -> "FAILED" to MaterialTheme.colorScheme.error
        StoryStatus.PENDING_REVIEW -> "REVIEW" to MaterialTheme.colorScheme.secondary
        StoryStatus.PENDING -> "PENDING" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
