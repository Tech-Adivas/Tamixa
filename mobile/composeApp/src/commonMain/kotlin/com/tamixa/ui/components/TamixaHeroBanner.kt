package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/**
 * Tamixa hero banner: full-width featured story with gradient overlay.
 * Large cover, title at bottom, play affordance.
 */
@Composable
fun TamixaHeroBanner(
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
            .clip(RoundedCornerShape(bottomStart = TamixaDesignTokens.dialogRadius, bottomEnd = TamixaDesignTokens.dialogRadius))
    ) {
        val itemWidth = maxWidth
        val listState = rememberLazyListState()
        if (displayStories.size > 1) {
            LaunchedEffect(displayStories.size) {
                while (true) {
                    delay(4000L)
                    val nextIndex = (listState.firstVisibleItemIndex + 1) % displayStories.size
                    listState.animateScrollToItem(nextIndex)
                }
            }
        }
        val snapLayoutInfoProvider = remember(listState) {
            SnapLayoutInfoProvider(listState, SnapPosition.Start)
        }
        val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            flingBehavior = flingBehavior,
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
                                colors = TamixaGradients.imageOverlay,
                                startY = 0f,
                                endY = overlayEndY
                            )
                        )
                )
                // Story title at bottom, play button beside it — vertically centered when title is one line
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(TamixaDesignTokens.contentPaddingHorizontal)
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = story.title?.takeIf { it.isNotBlank() } ?: story.theme,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (story.status == StoryStatus.READY) {
                        Box(
                            modifier = Modifier.clickable { onStoryClick(story) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        ambientColor = TamixaColors.goldAccent.copy(alpha = 0.4f),
                                        spotColor = TamixaColors.goldAccent.copy(alpha = 0.3f)
                                    )
                                    .background(TamixaColors.goldAccent, RoundedCornerShape(26.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = Strings.play(),
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
                }
            }
        }
        if (displayStories.size > 1) {
            val currentIndex by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) 0
                    else visibleItems.first().index
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                displayStories.forEachIndexed { index, _ ->
                    val selected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (selected) 22.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                color = if (selected) {
                                    TamixaColors.goldAccent
                                } else {
                                    Color.White.copy(alpha = 0.42f)
                                }
                            )
                    )
                }
            }
        }
        }
    }
}
