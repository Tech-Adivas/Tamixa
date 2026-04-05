package com.tamixa.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.platform.currentTimeMillis
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.StoryCoverImage
import com.tamixa.ui.components.TamixaHeroBanner
import com.tamixa.ui.components.TamixaMascot
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.isFunStory
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/**
 * Parent-facing home: vertical feed, soft sections, hub discovery chips — not a dense admin-style grid.
 * Background: [AppScreenBackground] Storybook Dusk.
 */
private val DashboardHeroHeight = 316.dp
/** Extra top inset after [statusBarsPadding]; 0 = flush with safe area. */
private val DashboardHeroOverlayTopPadding = 0.dp
/** Trailing space so the next poster peeks in carousel rows. */
private val DashboardCarouselPeekEnd = 52.dp
private val DashboardGridGutter = 12.dp
private val DashboardPosterRadius = 24.dp
private val DashboardBottomPadding = TamixaDesignTokens.screenPaddingBottomWithNav
private val DashboardCarouselPosterWidth = 142.dp

/** Snap-to-card + trailing peek for fixed-width poster carousels (continue / spotlight / picks). */
@Composable
private fun rememberDashboardPosterCarouselRowState(): Triple<LazyListState, FlingBehavior, PaddingValues> {
    val listState = rememberLazyListState()
    val snapProvider = remember(listState) {
        SnapLayoutInfoProvider(listState, SnapPosition.Start)
    }
    val flingBehavior = rememberSnapFlingBehavior(snapProvider)
    return Triple(listState, flingBehavior, PaddingValues(end = DashboardCarouselPeekEnd))
}

@Composable
private fun DashboardHomeSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.35).sp,
            ),
            color = Color(0xFFFFF4EC),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 5.dp),
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun DashboardSpotlightPill(
    label: String,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Poster tile: full-bleed cover, strong bottom scrim, centered title (reference mock).
 * Flat container (no card shadow); depth comes from the page background.
 */
@Composable
private fun DashboardPosterCard(
    story: Story,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    apiBaseUrl: String? = null,
    progress: Float? = null,
    showFunCornerChip: Boolean = false
) {
    val shape = RoundedCornerShape(DashboardPosterRadius)
    val ready = story.status == StoryStatus.READY
    Card(
        onClick = { if (ready) onClick() },
        enabled = ready,
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = TamixaColors.goldAccent.copy(alpha = 0.15f),
            )
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StoryCoverImage(
                story = story,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                apiBaseUrl = apiBaseUrl
            )
            if (!ready) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                0.78f to Color.Black.copy(alpha = 0.55f),
                                1f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            if (showFunCornerChip) {
                Text(
                    text = Strings.storyFunCornerBadge(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .zIndex(2f)
                        .shadow(
                            4.dp,
                            RoundedCornerShape(10.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.08f),
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TamixaColors.terracotta,
                                    TamixaColors.deepTeal.copy(alpha = 0.9f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (progress != null && progress in 0f..1f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = TamixaColors.goldAccent,
                        trackColor = Color.White.copy(alpha = 0.22f)
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = story.title?.takeIf { it.isNotBlank() } ?: story.theme,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Logo, greeting, and actions — drawn above the hero cover ([zIndex] applied by caller).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardHeroTopBar(
    greetingText: String,
    languageCode: String,
    usageStoriesUsed: Int,
    usageStoriesLimit: Int?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNewStory: () -> Unit,
    onNavigateToMyVoiceAndAvatar: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val topBarIconSize = 48.dp
    val logoSlot = 72.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(logoSlot),
            contentAlignment = Alignment.Center,
        ) {
            TamixaLanguageLogo(
                languageCode = languageCode,
                size = logoSlot,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = greetingText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.25).sp,
                    lineHeight = 28.sp,
                ),
                color = Color(0xFFFFF4EC),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (usageStoriesLimit != null || usageStoriesUsed > 0) {
                Text(
                    text = Strings.usageStoriesSummary(usageStoriesUsed, usageStoriesLimit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.height(logoSlot),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onNavigateToSearch,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.size(topBarIconSize),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = Strings.search(),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Box {
                Surface(
                    onClick = { menuExpanded = true },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.size(topBarIconSize),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = Strings.dashboardMenu(),
                            tint = Color.White,
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.createStory()) },
                        onClick = {
                            menuExpanded = false
                            onNewStory()
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.tabMyVoiceAndAvatar()) },
                        onClick = {
                            menuExpanded = false
                            onNavigateToMyVoiceAndAvatar()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Mic, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.settings()) },
                        onClick = {
                            menuExpanded = false
                            onNavigateToSettings()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.refresh()) },
                        onClick = {
                            menuExpanded = false
                            onRefresh()
                        },
                        enabled = !isRefreshing,
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroStreakAndSpark(listeningStreakDays: Int?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            listeningStreakDays != null && listeningStreakDays > 0 ->
                StreakBadge(days = listeningStreakDays, modifier = Modifier.padding(top = 4.dp))
            listeningStreakDays == null ->
                Text(
                    text = Strings.buildYourStreak(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 4.dp)
                )
        }
        val dayBucket = (currentTimeMillis() / 86_400_000L).toInt()
        val dailySpark = remember(dayBucket) { Strings.dashboardDailySparkAtIndex(dayBucket) }
        Text(
            text = dailySpark,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = Color.White.copy(alpha = 0.66f),
            modifier = Modifier.padding(top = 8.dp),
            lineHeight = 18.sp,
        )
    }
}

/** Streak module — chunky reward strip (Duolingo-like energy, Tamixa warm gradient). */
@Composable
private fun StreakBadge(days: Int, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "streakGlow")
    val glow by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    Box(
        modifier = modifier
            .shadow(
                elevation = (8f * glow).dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFFFFB020).copy(alpha = 0.35f),
                spotColor = TamixaColors.goldAccent.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(TamixaGradients.yellowOrangeButtonBrush())
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(
                text = Strings.listeningStreak(days),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp
                ),
                color = Color(0xFF2A1A0A)
            )
        }
    }
}

data class RecentPlaybackItem(
    val story: Story,
    val positionSeconds: Int,
    val storySource: String,
    /** From server enriched playback (0..1); falls back to estimate from reading time when null. */
    val progressFraction: Float? = null
)

data class RecommendedItem(
    val story: Story?,
    val title: String,
    val theme: String,
    val reason: String,
    val storyId: Long,
    val storySource: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    greeting: String,
    childName: String?,
    stories: List<Story>,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelect: (String) -> Unit,
    onStoryClick: (Story) -> Unit,
    onNewStory: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onNavigateToMyVoiceAndAvatar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    /** Opens Library with Fun corner filter (library?hub=fun). */
    onNavigateToLibraryFunCorner: () -> Unit = {},
    /** Opens Library on Learn & digital safety lane. */
    onNavigateToLibraryLearnSafety: () -> Unit = {},
    /** Opens Library Practice hub (Learn · Simulator / interactive graph). */
    onNavigateToLibrarySimulator: () -> Unit = {},
    /** Opens Life readiness snapshot (radar + suggestions). */
    onNavigateToLifeReadiness: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    recentPlayback: List<RecentPlaybackItem> = emptyList(),
    recommended: List<RecommendedItem> = emptyList(),
    apiBaseUrl: String? = null,
    languageCode: String = com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE,
    favoriteStoryIds: Set<Long> = emptySet(),
    onFavoriteToggle: (Story, Boolean) -> Unit = { _, _ -> },
    onRecommendedStoryClick: (Long, String) -> Unit = { _, _ -> },
    usageStoriesUsed: Int = 0,
    usageStoriesLimit: Int? = null,
    listeningStreakDays: Int? = null,
    /** Featured library listens (first slice of the full catalog—every story is learning-forward). */
    spotlightPreview: List<Story> = emptyList(),
    /** True while library list is loading and spotlight is still empty. */
    spotlightSectionLoading: Boolean = false,
    onSpotlightStoryClick: (Story) -> Unit = {},
) {
    val baseStories = if (stories.isEmpty()) com.tamixa.ui.data.SampleData.sampleStories() else stories
    val displayStories = baseStories
    val greetingText = if (childName != null) "$greeting, $childName!" else "$greeting!"
    val dedupedRecommended = recommended.distinctBy { it.storyId }
    val featuredStories = displayStories.sortedByDescending { it.createdAt }.take(5)
    val gridStories = displayStories.sortedByDescending { it.createdAt }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            TamixaBottomBar(
                selectedTab = com.tamixa.ui.components.TamixaTab.Home,
                onHome = { },
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppScreenBackground(
                modifier = Modifier.fillMaxSize(),
                showStars = true,
                showClouds = true,
                animateStars = true,
                ambientPresence = true
            )
            when {
                baseStories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(TamixaDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TamixaMascot(size = 120.dp)
                        Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
                        Text(
                            text = Strings.noStoriesYet(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                        Text(
                            text = Strings.browseLibraryForStories(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
                        TamixaPrimaryButton(
                            onClick = onNavigateToLibrary,
                            text = Strings.library(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    val storyRows = remember(gridStories) { gridStories.chunked(2) }
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = DashboardBottomPadding + 20.dp
                            ),
                        ) {
                            item(key = "hero") {
                                val heroStories = remember(featuredStories, dedupedRecommended, spotlightPreview) {
                                    val fromSpotlight = spotlightPreview.take(4)
                                    (fromSpotlight + featuredStories + dedupedRecommended.mapNotNull { it.story })
                                        .distinctBy { it.id }
                                        .filter { it.status == StoryStatus.READY }
                                        .take(10)
                                        .ifEmpty {
                                            if (featuredStories.isNotEmpty()) featuredStories
                                            else spotlightPreview.filter { it.status == StoryStatus.READY }.take(5)
                                        }
                                }
                                val heroChromeBrush = remember {
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color(0xFF120F0C).copy(alpha = 0.78f),
                                            0.22f to Color(0xFF120F0C).copy(alpha = 0.5f),
                                            0.45f to Color(0xFF120F0C).copy(alpha = 0.2f),
                                            0.62f to TamixaColors.goldAccent.copy(alpha = 0.06f),
                                            1f to Color.Transparent,
                                        ),
                                    )
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        TamixaHeroBanner(
                                            stories = heroStories,
                                            onStoryClick = onStoryClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .zIndex(0f),
                                            apiBaseUrl = apiBaseUrl,
                                            bannerHeight = DashboardHeroHeight,
                                            postContentOverlayBrush = heroChromeBrush,
                                            postContentFrameBorder = true,
                                        )
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .fillMaxWidth()
                                                .zIndex(1f)
                                                .statusBarsPadding()
                                                .padding(
                                                    start = TamixaDesignTokens.headerPaddingHorizontal,
                                                    end = TamixaDesignTokens.headerPaddingHorizontal,
                                                    top = DashboardHeroOverlayTopPadding,
                                                )
                                        ) {
                                            DashboardHeroTopBar(
                                                greetingText = greetingText,
                                                languageCode = languageCode,
                                                usageStoriesUsed = usageStoriesUsed,
                                                usageStoriesLimit = usageStoriesLimit,
                                                isRefreshing = isRefreshing,
                                                onRefresh = onRefresh,
                                                onNavigateToSearch = onNavigateToSearch,
                                                onNavigateToSettings = onNavigateToSettings,
                                                onNewStory = onNewStory,
                                                onNavigateToMyVoiceAndAvatar = onNavigateToMyVoiceAndAvatar,
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                                            .padding(top = 12.dp, bottom = 4.dp)
                                    ) {
                                        DashboardHeroStreakAndSpark(listeningStreakDays = listeningStreakDays)
                                    }
                                }
                            }
                            item(key = "post_hero_spacer") {
                                Spacer(Modifier.height(4.dp))
                            }
                            if (recentPlayback.isNotEmpty()) {
                                item(key = "continue_row") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                                    ) {
                                        val (continueState, continueFling, continuePad) =
                                            rememberDashboardPosterCarouselRowState()
                                        Spacer(Modifier.height(28.dp))
                                        DashboardHomeSectionHeader(
                                            title = Strings.continueListening(),
                                            subtitle = Strings.latestListeningSubtitle(),
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        LazyRow(
                                            state = continueState,
                                            flingBehavior = continueFling,
                                            horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                                            contentPadding = continuePad,
                                        ) {
                                            items(
                                                recentPlayback,
                                                key = { "${it.story.id}-${it.storySource}" },
                                            ) { item ->
                                                val progress = item.progressFraction?.takeIf { it in 0f..1f }
                                                    ?: if (item.story.readingTimeMinutes > 0) {
                                                        (item.positionSeconds / 60.0 / item.story.readingTimeMinutes)
                                                            .toFloat()
                                                            .coerceIn(0f, 1f)
                                                    } else {
                                                        null
                                                    }
                                                DashboardPosterCard(
                                                    story = item.story,
                                                    onClick = { onStoryClick(item.story) },
                                                    modifier = Modifier
                                                        .width(DashboardCarouselPosterWidth)
                                                        .aspectRatio(3f / 4f),
                                                    apiBaseUrl = apiBaseUrl,
                                                    progress = progress,
                                                    showFunCornerChip = isFunStory(item.story),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item(key = "spotlight_block") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                                ) {
                                Spacer(Modifier.height(14.dp))
                                DashboardHomeSectionHeader(
                                    title = Strings.dashboardSpotlightTitle(),
                                    subtitle = Strings.dashboardSpotlightSubtitle(),
                                )
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(end = 28.dp),
                                ) {
                                    item {
                                        DashboardSpotlightPill(
                                            label = Strings.openLibraryForMore(),
                                            containerColor = TamixaColors.goldAccent.copy(alpha = 0.24f),
                                            borderColor = TamixaColors.goldAccent.copy(alpha = 0.5f),
                                            onClick = onNavigateToLibrary,
                                        )
                                    }
                                    item {
                                        DashboardSpotlightPill(
                                            label = Strings.openFunCorner(),
                                            containerColor = TamixaColors.skyBlue.copy(alpha = 0.2f),
                                            borderColor = TamixaColors.skyBlue.copy(alpha = 0.52f),
                                            onClick = onNavigateToLibraryFunCorner,
                                        )
                                    }
                                    item {
                                        DashboardSpotlightPill(
                                            label = Strings.openLearnSafety(),
                                            containerColor = TamixaColors.deepTeal.copy(alpha = 0.26f),
                                            borderColor = Color.White.copy(alpha = 0.3f),
                                            onClick = onNavigateToLibraryLearnSafety,
                                        )
                                    }
                                    item {
                                        DashboardSpotlightPill(
                                            label = Strings.openPracticeHub(),
                                            containerColor = Color(0xFF2D6A4F).copy(alpha = 0.35f),
                                            borderColor = Color.White.copy(alpha = 0.28f),
                                            onClick = onNavigateToLibrarySimulator,
                                        )
                                    }
                                    item {
                                        DashboardSpotlightPill(
                                            label = Strings.lifeReadinessPillLabel(),
                                            containerColor = TamixaColors.goldAccent.copy(alpha = 0.18f),
                                            borderColor = TamixaColors.goldAccent.copy(alpha = 0.45f),
                                            onClick = onNavigateToLifeReadiness,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                when {
                                    spotlightSectionLoading -> {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = TamixaColors.goldAccent,
                                            trackColor = Color.White.copy(alpha = 0.12f),
                                        )
                                    }
                                    spotlightPreview.isEmpty() -> {
                                        Text(
                                            text = Strings.spotlightEmptyHint(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.68f),
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                    }
                                    else -> {
                                        val (spotlightState, spotlightFling, spotlightPad) =
                                            rememberDashboardPosterCarouselRowState()
                                        LazyRow(
                                            state = spotlightState,
                                            flingBehavior = spotlightFling,
                                            horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                                            contentPadding = spotlightPad,
                                        ) {
                                            items(spotlightPreview, key = { it.id }) { s ->
                                                DashboardPosterCard(
                                                    story = s,
                                                    onClick = { onSpotlightStoryClick(s) },
                                                    modifier = Modifier
                                                        .width(DashboardCarouselPosterWidth)
                                                        .aspectRatio(3f / 4f),
                                                    apiBaseUrl = apiBaseUrl,
                                                    showFunCornerChip = isFunStory(s),
                                                )
                                            }
                                        }
                                    }
                                }
                                }
                            }
                            if (dedupedRecommended.isNotEmpty()) {
                                item(key = "recommended") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                                    ) {
                                        val (recState, recFling, recPad) = rememberDashboardPosterCarouselRowState()
                                        Spacer(Modifier.height(28.dp))
                                        DashboardHomeSectionHeader(
                                            title = Strings.recommendedForYou(),
                                            subtitle = null,
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        LazyRow(
                                            state = recState,
                                            flingBehavior = recFling,
                                            horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                                            contentPadding = recPad,
                                        ) {
                                            items(dedupedRecommended, key = { it.storyId }) { item ->
                                                val story = item.story
                                                val displayStory = story ?: Story(
                                                    id = item.storyId,
                                                    parentId = 0L,
                                                    childId = 0L,
                                                    content = "",
                                                    theme = item.theme,
                                                    title = item.title,
                                                    language = languageCode,
                                                    age = 5,
                                                    childName = "",
                                                    wordCount = 0,
                                                    readingTimeMinutes = 0.0,
                                                    status = StoryStatus.READY,
                                                    moral = null,
                                                    audioFileUrl = null,
                                                    coverImageUrl = null,
                                                    createdAt = "",
                                                )
                                                DashboardPosterCard(
                                                    story = displayStory,
                                                    onClick = {
                                                        onRecommendedStoryClick(item.storyId, item.storySource)
                                                    },
                                                    modifier = Modifier
                                                        .width(DashboardCarouselPosterWidth)
                                                        .aspectRatio(3f / 4f),
                                                    apiBaseUrl = apiBaseUrl,
                                                    showFunCornerChip = isFunStory(displayStory),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item(key = "your_stories_header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                                ) {
                                    Spacer(Modifier.height(28.dp))
                                    DashboardHomeSectionHeader(
                                        title = Strings.dashboardYourStoriesTitle(),
                                        subtitle = Strings.dashboardYourStoriesSubtitle(),
                                    )
                                }
                            }
                            itemsIndexed(
                                items = storyRows,
                                key = { _, row -> row.first().id },
                            ) { index, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                                        .padding(top = if (index == 0) 12.dp else DashboardGridGutter),
                                    horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                                ) {
                                    row.forEach { story ->
                                        DashboardPosterCard(
                                            story = story,
                                            onClick = { onStoryClick(story) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(3f / 4f),
                                            apiBaseUrl = apiBaseUrl,
                                            showFunCornerChip = isFunStory(story),
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreenPreview() {
    com.tamixa.ui.theme.TamixaTheme {
        DashboardScreen(
            greeting = com.tamixa.ui.strings.Strings.goodEvening(),
            childName = null,
            stories = com.tamixa.ui.data.SampleData.sampleStories(),
            selectedCategory = "All",
            categories = com.tamixa.ui.data.SampleData.categories,
            onCategorySelect = {},
            onStoryClick = {},
            onNewStory = {},
            onRefresh = {},
            isRefreshing = false,
            onNavigateToMyVoiceAndAvatar = {},
            onNavigateToSettings = {},
            onNavigateToSearch = {},
            recommended = emptyList(),
            languageCode = com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE,
            favoriteStoryIds = emptySet(),
            onRecommendedStoryClick = { _, _ -> },
            onFavoriteToggle = { _, _ -> },
            usageStoriesUsed = 2,
            usageStoriesLimit = 5,
            listeningStreakDays = null
        )
    }
}
