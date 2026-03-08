package com.araro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.theme.AraroGradients

/**
 * Araro hero banner: full-width featured story with gradient overlay.
 * Large cover, title at bottom, play affordance.
 */
@Composable
fun AraroHeroBanner(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    modifier: Modifier = Modifier,
    apiBaseUrl: String? = null
) {
    if (stories.isEmpty()) return
    val readyStories = stories.filter { it.status == StoryStatus.READY }
    val displayStories = if (readyStories.isEmpty()) stories else readyStories
    val density = LocalDensity.current
    val overlayEndY = with(density) { 220.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(bottomStart = AraroDesignTokens.dialogRadius, bottomEnd = AraroDesignTokens.dialogRadius))
    ) {
        val itemWidth = maxWidth
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(displayStories) { story ->
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(220.dp)
                ) {
                StoryCoverImage(
                    story = story,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop,
                    apiBaseUrl = apiBaseUrl
                )
                // Gradient overlay (transparent → dark at bottom)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = AraroGradients.imageOverlay,
                                startY = 0f,
                                endY = overlayEndY
                            )
                        )
                )
                // Title + play at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(AraroDesignTokens.contentPaddingHorizontal)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = story.theme,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (story.status == StoryStatus.READY) {
                        Row(
                            modifier = Modifier.clickable { onStoryClick(story) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AraroColors.goldAccent, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "${story.readingTimeMinutes.toInt()} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
