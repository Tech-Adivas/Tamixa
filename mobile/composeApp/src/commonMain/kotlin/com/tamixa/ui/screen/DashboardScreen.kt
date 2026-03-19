package com.tamixa.ui.screen

import com.tamixa.ui.components.AppScreenBackground
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaHeroBanner
import com.tamixa.ui.components.TamixaMascot
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.components.StoryCard
import com.tamixa.ui.components.StoryCarouselCard
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.luminance

// Spacing: design tokens for enterprise consistency
private val DashboardSectionSpacing = TamixaDesignTokens.sectionSpacing
private val DashboardHeaderToContent = TamixaDesignTokens.cardSpacing
private val DashboardBottomPadding = TamixaDesignTokens.screenPaddingBottomWithNav
private val GridNarrowBreakpoint = 400.dp

/** Enterprise section wrapper: consistent padding, solid background (no dividers) */
@Composable
private fun DashboardSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal)
                .padding(top = DashboardSectionSpacing, bottom = 0.dp),
            content = content
        )
    }
}

/** Enterprise section header: refined typography, Material icon */
@Composable
private fun DashboardSectionHeader(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 10.dp),
                tint = TamixaColors.goldAccent
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = textColor
        )
    }
}

/** Streak badge — refined, enterprise-style */
@Composable
private fun StreakBadge(days: Int, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "streakGlow")
    val glow by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    Surface(
        modifier = modifier.alpha(glow),
        shape = RoundedCornerShape(12.dp),
        color = TamixaColors.goldAccent.copy(alpha = 0.18f),
        border = BorderStroke(
            1.dp,
            TamixaColors.goldAccent.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = Strings.listeningStreak(days),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = TamixaColors.goldAccent
            )
        }
    }
}

data class RecentPlaybackItem(
    val story: Story,
    val positionSeconds: Int,
    val storySource: String
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
    val featuredStories = displayStories.sortedByDescending { it.createdAt }.take(5)
    val storyOfTheDay = featuredStories.firstOrNull()
    val dedupedRecommended = recommended.distinctBy { it.storyId }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    Scaffold(
        containerColor = if (isDark) colorScheme.background else TamixaColors.appBgTop,
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showStars = true, showClouds = true, animateStars = true)
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                        Text(
                            text = Strings.browseLibraryForStories(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
                        Button(
                            onClick = onNavigateToLibrary,
                            colors = ButtonDefaults.buttonColors(containerColor = TamixaColors.goldAccent),
                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = Strings.library(),
                                color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
                else -> {
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = DashboardBottomPadding),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val heroStories = (featuredStories + dedupedRecommended.mapNotNull { it.story })
                                    .distinctBy { it.id }
                                    .filter { it.status == StoryStatus.READY }
                                    .take(10)
                                    .ifEmpty { featuredStories }
                                TamixaHeroBanner(
                                    stories = heroStories,
                                    onStoryClick = onStoryClick,
                                    apiBaseUrl = apiBaseUrl
                                )
                                // Enterprise header overlay: strong gradient for logo + greeting visibility
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f),
                                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
                                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.15f),
                                                    androidx.compose.ui.graphics.Color.Transparent
                                                )
                                            )
                                        )
                                        .padding(
                                            horizontal = TamixaDesignTokens.headerPaddingHorizontal,
                                            vertical = TamixaDesignTokens.headerPaddingVertical
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TamixaLanguageLogo(
                                                languageCode = languageCode,
                                                size = 80.dp
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = greetingText,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = (-0.2).sp
                                                ),
                                                color = androidx.compose.ui.graphics.Color.White
                                            )
                                            if (usageStoriesLimit != null || usageStoriesUsed > 0) {
                                                Text(
                                                    text = Strings.usageStoriesSummary(usageStoriesUsed, usageStoriesLimit),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f)
                                                )
                                            }
                                        }
                                        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = Strings.refresh(),
                                                tint = if (isRefreshing) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f) else androidx.compose.ui.graphics.Color.White
                                            )
                                        }
                                        IconButton(onClick = onNavigateToSearch) {
                                            Icon(
                                                Icons.Outlined.Search,
                                                contentDescription = Strings.search(),
                                                tint = androidx.compose.ui.graphics.Color.White
                                            )
                                        }
                                    }
                                    if (listeningStreakDays != null && listeningStreakDays > 0) {
                                        StreakBadge(days = listeningStreakDays, modifier = Modifier.padding(top = 8.dp))
                                    } else if (listeningStreakDays == null) {
                                        Text(
                                            text = Strings.buildYourStreak(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (storyOfTheDay != null) {
                            item {
                                DashboardSection {
                                    DashboardSectionHeader(
                                        title = Strings.storyOfTheDay(),
                                        icon = Icons.Filled.Star
                                    )
                                    Spacer(Modifier.height(DashboardHeaderToContent))
                                    StoryCard(
                                        story = storyOfTheDay,
                                        onClick = { onStoryClick(storyOfTheDay) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = TamixaColors.goldAccent.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(TamixaDesignTokens.cardRadius)
                                            ),
                                        apiBaseUrl = apiBaseUrl,
                                        isFavorite = storyOfTheDay.id in favoriteStoryIds,
                                        onFavoriteClick = { onFavoriteToggle(storyOfTheDay, storyOfTheDay.id !in favoriteStoryIds) },
                                        showPlayButton = true
                                    )
                                }
                            }
                        }
                        if (recentPlayback.isNotEmpty()) {
                            item {
                                DashboardSection {
                                    DashboardSectionHeader(
                                        title = Strings.continueListening(),
                                        icon = Icons.Filled.Headphones
                                    )
                                    Spacer(Modifier.height(DashboardHeaderToContent))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing),
                                        contentPadding = PaddingValues(end = TamixaDesignTokens.contentPaddingHorizontal)
                                    ) {
                                        items(recentPlayback) { item ->
                                            val progress = if (item.story.readingTimeMinutes > 0) {
                                                (item.positionSeconds / 60.0 / item.story.readingTimeMinutes).toFloat().coerceIn(0f, 1f)
                                            } else null
                                            StoryCarouselCard(
                                                story = item.story,
                                                onClick = { onStoryClick(item.story) },
                                                apiBaseUrl = apiBaseUrl,
                                                width = 160.dp,
                                                height = 232.dp,
                                                progress = progress
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (dedupedRecommended.isNotEmpty()) {
                            item {
                                DashboardSection {
                                    DashboardSectionHeader(
                                        title = Strings.recommendedForYou(),
                                        icon = Icons.Filled.AutoAwesome
                                    )
                                    Spacer(Modifier.height(DashboardHeaderToContent))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing),
                                        contentPadding = PaddingValues(end = TamixaDesignTokens.contentPaddingHorizontal)
                                    ) {
                                        items(dedupedRecommended) { item ->
                                            val story = item.story
                                            val onClick = { onRecommendedStoryClick(item.storyId, item.storySource) }
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
                                            StoryCarouselCard(
                                                story = displayStory,
                                                onClick = onClick,
                                                apiBaseUrl = apiBaseUrl,
                                                width = 160.dp,
                                                height = 232.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            DashboardSection {
                                DashboardSectionHeader(
                                    title = Strings.trending(),
                                    icon = Icons.AutoMirrored.Filled.TrendingUp
                                )
                                Spacer(Modifier.height(DashboardHeaderToContent))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing),
                                    contentPadding = PaddingValues(
                                        start = 0.dp,
                                        end = TamixaDesignTokens.contentPaddingHorizontal
                                    )
                                ) {
                                    items(featuredStories) { story ->
                                        StoryCarouselCard(
                                            story = story,
                                            onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                            apiBaseUrl = apiBaseUrl,
                                            width = 160.dp,
                                            height = 232.dp
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            DashboardSection {
                                DashboardSectionHeader(
                                    title = Strings.allStories(),
                                    icon = Icons.AutoMirrored.Outlined.MenuBook
                                )
                                Spacer(Modifier.height(DashboardHeaderToContent))
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val gridSpacing = TamixaDesignTokens.cardSpacing
                                    val colCount = if (maxWidth < GridNarrowBreakpoint) 2 else 3
                                    val gapTotal = gridSpacing * (colCount - 1)
                                    val cardWidth = (maxWidth - gapTotal) / colCount
                                    val cardHeight = cardWidth * 232 / 160
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = DashboardSectionSpacing),
                                        verticalArrangement = Arrangement.spacedBy(gridSpacing)
                                    ) {
                                        displayStories.chunked(colCount).forEach { rowStories ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                                            ) {
                                                rowStories.forEach { story ->
                                                    StoryCarouselCard(
                                                        story = story,
                                                        onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                                        apiBaseUrl = apiBaseUrl,
                                                        width = cardWidth,
                                                        height = cardHeight
                                                    )
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
