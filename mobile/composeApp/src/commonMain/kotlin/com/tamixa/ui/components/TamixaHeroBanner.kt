package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tamixa.platform.currentTimeMillis
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

private const val HeroAutoAdvanceIntervalMs = 4_000L
/** After a manual swipe, wait this long before auto-advance may run again. */
private const val HeroCooldownAfterUserScrollMs = 3_500L
/** Ignore scroll snapshots briefly after programmatic scroll so we don’t treat them as user input. */
private const val HeroProgrammaticSettleMs = 150L

/**
 * Tamixa hero banner: full-width featured story with gradient overlay.
 * Large cover, title at bottom, play affordance.
 *
 * @param postContentOverlayBrush When non-null, drawn above carousel content (paint-only; does not block taps).
 * @param postContentFrameBorder Thin frame matching [TamixaDesignTokens.heroBannerShape] (e.g. dashboard).
 */
@Composable
fun TamixaHeroBanner(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    modifier: Modifier = Modifier,
    apiBaseUrl: String? = null,
    /** Taller = more cinematic on home; default keeps other call sites unchanged. */
    bannerHeight: Dp = 220.dp,
    postContentOverlayBrush: Brush? = null,
    postContentFrameBorder: Boolean = false,
) {
    val heroShape = TamixaDesignTokens.heroBannerShape
    val frameModifier =
        if (postContentFrameBorder) {
            Modifier.border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = heroShape,
            )
        } else {
            Modifier
        }
    if (stories.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .clip(heroShape)
                .then(frameModifier)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF2A2420),
                            1f to Color(0xFF151210),
                        ),
                    )
                )
        )
        return
    }
    val readyStories = stories.filter { it.status == StoryStatus.READY }
    val displayStories = if (readyStories.isEmpty()) stories else readyStories
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val overlayEndY = with(density) { bannerHeight.toPx() }
    val dotBottomPadding = remember(fontScale) {
        val extra = ((fontScale - 1f).coerceAtLeast(0f) * 12f).dp
        (14.dp + extra).coerceAtMost(32.dp)
    }

    val heroTitleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.55f),
        offset = Offset(0f, 1.5f),
        blurRadius = 10f,
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .clip(heroShape)
            .then(frameModifier)
    ) {
        val itemWidth = maxWidth
        val listState = rememberLazyListState()
        var lastUserInteractionMillis by remember { mutableLongStateOf(0L) }
        var programmaticScrolling by remember { mutableStateOf(false) }
        if (displayStories.size > 1) {
            LaunchedEffect(displayStories, listState) {
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }.collect { (_, _) ->
                    if (!programmaticScrolling) {
                        lastUserInteractionMillis = currentTimeMillis()
                    }
                }
            }
            LaunchedEffect(displayStories, listState) {
                while (true) {
                    delay(HeroAutoAdvanceIntervalMs)
                    if (listState.firstVisibleItemScrollOffset != 0) continue
                    val now = currentTimeMillis()
                    if (lastUserInteractionMillis != 0L &&
                        now - lastUserInteractionMillis < HeroCooldownAfterUserScrollMs
                    ) {
                        continue
                    }
                    val nextIndex = (listState.firstVisibleItemIndex + 1) % displayStories.size
                    programmaticScrolling = true
                    try {
                        listState.animateScrollToItem(nextIndex)
                        delay(HeroProgrammaticSettleMs)
                    } finally {
                        programmaticScrolling = false
                    }
                }
            }
        }
        val snapLayoutInfoProvider = remember(listState) {
            SnapLayoutInfoProvider(listState, SnapPosition.Start)
        }
        val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)
        val overlayBrush = postContentOverlayBrush
        val carouselModifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .then(
                if (overlayBrush != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(brush = overlayBrush)
                    }
                } else {
                    Modifier
                }
            )
        Box(modifier = carouselModifier) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                flingBehavior = flingBehavior,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(displayStories) { story ->
                    val displayTitle = story.title?.takeIf { it.isNotBlank() } ?: story.theme
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(bannerHeight)
                    ) {
                        StoryCoverImage(
                            story = story,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bannerHeight),
                            contentScale = ContentScale.Crop,
                            apiBaseUrl = apiBaseUrl
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(bannerHeight * 0.42f)
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Black.copy(alpha = 0.38f),
                                            1f to Color.Transparent,
                                        ),
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bannerHeight)
                                .background(
                                    Brush.verticalGradient(
                                        colors = TamixaGradients.imageOverlay,
                                        startY = 0f,
                                        endY = overlayEndY
                                    )
                                )
                        )
                        val rowInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(TamixaDesignTokens.contentPaddingHorizontal)
                                .padding(bottom = 20.dp)
                                .then(
                                    if (story.status == StoryStatus.READY) {
                                        Modifier
                                            .semantics(mergeDescendants = true) {
                                                contentDescription =
                                                    "$displayTitle, ${Strings.openStoryAction()}"
                                                role = Role.Button
                                            }
                                            .clickable(
                                                interactionSource = rowInteraction,
                                                indication = null,
                                            ) {
                                                onStoryClick(story)
                                            }
                                    } else {
                                        Modifier
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(shadow = heroTitleShadow),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (story.status == StoryStatus.READY) {
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center
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
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(26.dp),
                                            )
                                            .background(TamixaColors.goldAccent, RoundedCornerShape(26.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
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
                        listState.firstVisibleItemIndex.coerceIn(0, displayStories.lastIndex)
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = dotBottomPadding)
                        .semantics(mergeDescendants = true) {
                            contentDescription = Strings.featuredStoriesPager(
                                currentIndex + 1,
                                displayStories.size
                            )
                        },
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
