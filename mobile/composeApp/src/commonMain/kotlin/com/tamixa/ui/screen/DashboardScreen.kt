package com.tamixa.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.StoryCoverImage
import com.tamixa.ui.components.TamixaHeroBanner
import com.tamixa.ui.components.TamixaMascot
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/** Dark streaming-style home: poster grid over Storybook Dusk animated sky (see [AppScreenBackground]). */
private val DashboardGridGutter = 10.dp
private val DashboardPosterRadius = 18.dp
private val DashboardBottomPadding = TamixaDesignTokens.screenPaddingBottomWithNav
private val DashboardCarouselPosterWidth = 118.dp

@Composable
private fun DashboardCinemaSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        ),
        color = Color.White.copy(alpha = 0.92f)
    )
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
    progress: Float? = null
) {
    val shape = RoundedCornerShape(DashboardPosterRadius)
    val ready = story.status == StoryStatus.READY
    Card(
        onClick = { if (ready) onClick() },
        enabled = ready,
        modifier = modifier.clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

@Composable
private fun DashboardCinemaHeader(
    greetingText: String,
    languageCode: String,
    usageStoriesUsed: Int,
    usageStoriesLimit: Int?,
    listeningStreakDays: Int?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TamixaLanguageLogo(languageCode = languageCode, size = 56.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.25).sp,
                        lineHeight = 28.sp
                    ),
                    color = Color.White
                )
                if (usageStoriesLimit != null || usageStoriesUsed > 0) {
                    Text(
                        text = Strings.usageStoriesSummary(usageStoriesUsed, usageStoriesLimit),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = Strings.refresh(),
                    tint = if (isRefreshing) Color.White.copy(alpha = 0.55f) else Color.White
                )
            }
            IconButton(onClick = onNavigateToSearch) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = Strings.search(),
                    tint = Color.White
                )
            }
        }
        when {
            listeningStreakDays != null && listeningStreakDays > 0 ->
                StreakBadge(days = listeningStreakDays, modifier = Modifier.padding(top = 10.dp))
            listeningStreakDays == null ->
                Text(
                    text = Strings.buildYourStreak(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 10.dp)
                )
        }
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
    listeningStreakDays: Int? = null
) {
    val baseStories = if (stories.isEmpty()) com.tamixa.ui.data.SampleData.sampleStories() else stories
    val displayStories = baseStories
    val greetingText = if (childName != null) "$greeting, $childName!" else "$greeting!"
    val dedupedRecommended = recommended.distinctBy { it.storyId }
    val featuredStories = displayStories.sortedByDescending { it.createdAt }.take(5)
    val gridStories = displayStories.sortedByDescending { it.createdAt }

    Scaffold(
        containerColor = TamixaColors.nightSkyBg,
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
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = DashboardBottomPadding + 12.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                            verticalArrangement = Arrangement.spacedBy(DashboardGridGutter)
                        ) {
                            item(span = { GridItemSpan(3) }) {
                                val heroStories =
                                    (featuredStories + dedupedRecommended.mapNotNull { it.story })
                                        .distinctBy { it.id }
                                        .filter { it.status == StoryStatus.READY }
                                        .take(10)
                                        .ifEmpty { featuredStories }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    TamixaHeroBanner(
                                        stories = heroStories,
                                        onStoryClick = onStoryClick,
                                        modifier = Modifier.fillMaxWidth(),
                                        apiBaseUrl = apiBaseUrl
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.78f),
                                                        Color.Black.copy(alpha = 0.42f),
                                                        TamixaColors.goldAccent.copy(alpha = 0.18f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .padding(
                                                horizontal = TamixaDesignTokens.headerPaddingHorizontal,
                                                vertical = TamixaDesignTokens.headerPaddingVertical
                                            )
                                    ) {
                                        DashboardCinemaHeader(
                                            greetingText = greetingText,
                                            languageCode = languageCode,
                                            usageStoriesUsed = usageStoriesUsed,
                                            usageStoriesLimit = usageStoriesLimit,
                                            listeningStreakDays = listeningStreakDays,
                                            isRefreshing = isRefreshing,
                                            onRefresh = onRefresh,
                                            onNavigateToSearch = onNavigateToSearch
                                        )
                                    }
                                }
                            }
                            if (recentPlayback.isNotEmpty()) {
                                item(span = { GridItemSpan(3) }) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DashboardCinemaSectionTitle(Strings.continueListening())
                                        Text(
                                            text = Strings.latestListeningSubtitle(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.72f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                                            contentPadding = PaddingValues(end = 4.dp)
                                        ) {
                                            items(
                                                recentPlayback,
                                                key = { "${it.story.id}-${it.storySource}" }
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
                                                    progress = progress
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (dedupedRecommended.isNotEmpty()) {
                                item(span = { GridItemSpan(3) }) {
                                    Column(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DashboardCinemaSectionTitle(Strings.recommendedForYou())
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(DashboardGridGutter),
                                            contentPadding = PaddingValues(end = 4.dp)
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
                                                    createdAt = ""
                                                )
                                                DashboardPosterCard(
                                                    story = displayStory,
                                                    onClick = {
                                                        onRecommendedStoryClick(item.storyId, item.storySource)
                                                    },
                                                    modifier = Modifier
                                                        .width(DashboardCarouselPosterWidth)
                                                        .aspectRatio(3f / 4f),
                                                    apiBaseUrl = apiBaseUrl
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item(span = { GridItemSpan(3) }) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    DashboardCinemaSectionTitle(Strings.allStories())
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            items(gridStories, key = { it.id }) { story ->
                                DashboardPosterCard(
                                    story = story,
                                    onClick = { onStoryClick(story) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f),
                                    apiBaseUrl = apiBaseUrl
                                )
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
