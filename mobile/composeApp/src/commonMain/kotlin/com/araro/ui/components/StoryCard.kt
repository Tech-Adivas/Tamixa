package com.araro.ui.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens

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
    onFavoriteClick: (() -> Unit)? = null
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
            color = AraroColors.goldAccent.copy(alpha = shimmerAlpha),
            shape = RoundedCornerShape(AraroDesignTokens.dialogRadius)
        )
    } else Modifier

    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = { if (story.status == StoryStatus.READY) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .scale(entranceScale)
            .alpha(entranceAlpha),
        shape = RoundedCornerShape(AraroDesignTokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = AraroDesignTokens.cardElevation),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(
                        topStart = AraroDesignTokens.cardRadius,
                        bottomStart = AraroDesignTokens.cardRadius
                    ))
                    .background(colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                effectiveThumbnail()
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.theme,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${story.readingTimeMinutes.toInt()} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AraroColors.goldAccent.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = story.language.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = AraroColors.goldAccent,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (story.status != StoryStatus.READY) {
                        Spacer(Modifier.height(4.dp))
                        StatusChip(status = story.status)
                    }
                }
                if (story.status == StoryStatus.READY) {
                    Surface(
                        shape = CircleShape,
                        color = AraroColors.goldAccent,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = Strings.play(),
                            tint = androidx.compose.ui.graphics.Color.Black,
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AraroColors.goldAccent
            )
        }
            if (onFavoriteClick != null && story.status == StoryStatus.READY) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = Strings.favorites(),
                        tint = if (isFavorite) AraroColors.goldAccent else colorScheme.onSurfaceVariant
                    )
                }
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
        StoryStatus.PENDING -> "PENDING" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
