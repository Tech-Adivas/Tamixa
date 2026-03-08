package com.araro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.components.AraroLanguageLogo
import com.araro.ui.components.AraroHeroBanner
import com.araro.ui.components.AraroStoryFilter
import com.araro.ui.components.StoryCard
import com.araro.ui.components.StoryCarouselCard
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens

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
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    recentPlayback: List<RecentPlaybackItem> = emptyList(),
    recommended: List<RecommendedItem> = emptyList(),
    apiBaseUrl: String? = null,
    languageCode: String = com.araro.util.AraroConstants.DEFAULT_LANGUAGE,
    favoriteStoryIds: Set<Long> = emptySet(),
    onFavoriteToggle: (Story, Boolean) -> Unit = { _, _ -> },
    onRecommendedStoryClick: (Long, String) -> Unit = { _, _ -> }
) {
    val baseStories = if (stories.isEmpty()) com.araro.ui.data.SampleData.sampleStories() else stories
    val displayStories = filterStoriesByCategory(baseStories, selectedCategory)
    val greetingText = if (childName != null) "$greeting, $childName!" else "$greeting!"
    val featuredStories = displayStories.sortedByDescending { it.createdAt }.take(5)
    val readyStories = displayStories.filter { it.status == StoryStatus.READY }

    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, contentDescription = Strings.home(), tint = colorScheme.onSurface) },
                    label = { Text(Strings.home(), color = colorScheme.onSurface) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCategories,
                    icon = { Icon(Icons.Outlined.Category, contentDescription = Strings.categories(), tint = colorScheme.onSurfaceVariant) },
                    label = { Text(Strings.categories(), color = colorScheme.onSurfaceVariant) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToFavorites,
                    icon = { Icon(Icons.Outlined.Favorite, contentDescription = Strings.favorites(), tint = colorScheme.onSurfaceVariant) },
                    label = { Text(Strings.favorites(), color = colorScheme.onSurfaceVariant) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettings,
                    icon = { Icon(Icons.Default.Person, contentDescription = Strings.parent(), tint = colorScheme.onSurfaceVariant) },
                    label = { Text(Strings.parent(), color = colorScheme.onSurfaceVariant) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                displayStories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AraroDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AraroEmojiDisplay(emoji = "📚✨", fontSize = 72.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = Strings.noStoriesYet(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Strings.tapNewStoryToCreate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                .padding(bottom = 88.dp),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AraroHeroBanner(
                                    stories = featuredStories,
                                    onStoryClick = onStoryClick,
                                    apiBaseUrl = apiBaseUrl
                                )
                                // Minimal top bar overlay
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = AraroDesignTokens.headerPaddingHorizontal,
                                            vertical = AraroDesignTokens.headerPaddingVertical
                                        )
                                        .background(
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AraroLanguageLogo(
                                        languageCode = languageCode,
                                        size = 40.dp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = greetingText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AraroColors.cream,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = Strings.refresh(),
                                            tint = if (isRefreshing) AraroColors.cream.copy(alpha = 0.7f) else AraroColors.cream
                                        )
                                    }
                                    IconButton(onClick = onNavigateToSearch) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = Strings.search(),
                                            tint = AraroColors.cream
                                        )
                                    }
                                    IconButton(onClick = onSettings) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = Strings.settings(),
                                            tint = AraroColors.cream
                                        )
                                    }
                                }
                            }
                        }
                        if (recentPlayback.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colorScheme.background)
                                        .padding(horizontal = AraroDesignTokens.contentPaddingHorizontal)
                                        .padding(top = AraroDesignTokens.sectionSpacing)
                                ) {
                                    Text(
                                        text = Strings.continueListening(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(AraroDesignTokens.cardSpacing),
                                        contentPadding = PaddingValues(end = AraroDesignTokens.contentPaddingHorizontal)
                                    ) {
                                        items(recentPlayback) { item ->
                                            val progress = if (item.story.readingTimeMinutes > 0) {
                                                (item.positionSeconds / 60.0 / item.story.readingTimeMinutes).toFloat().coerceIn(0f, 1f)
                                            } else 0f
                                            StoryCard(
                                                story = item.story,
                                                onClick = { onStoryClick(item.story) },
                                                progress = progress,
                                                modifier = Modifier.width(280.dp),
                                                apiBaseUrl = apiBaseUrl,
                                                isFavorite = item.story.id in favoriteStoryIds,
                                                onFavoriteClick = { onFavoriteToggle(item.story, item.story.id !in favoriteStoryIds) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (recommended.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colorScheme.background)
                                        .padding(horizontal = AraroDesignTokens.contentPaddingHorizontal)
                                        .padding(top = AraroDesignTokens.sectionSpacing)
                                ) {
                                    Text(
                                        text = Strings.recommendedForYou(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(AraroDesignTokens.cardSpacing),
                                        contentPadding = PaddingValues(end = AraroDesignTokens.contentPaddingHorizontal)
                                    ) {
                                        items(recommended) { item ->
                                            val story = item.story
                                            val onClick = { onRecommendedStoryClick(item.storyId, item.storySource) }
                                            if (story != null) {
                                                StoryCard(
                                                    story = story,
                                                    onClick = onClick,
                                                    modifier = Modifier.width(280.dp),
                                                    apiBaseUrl = apiBaseUrl,
                                                    isFavorite = story.id in favoriteStoryIds,
                                                    onFavoriteClick = { onFavoriteToggle(story, story.id !in favoriteStoryIds) }
                                                )
                                            } else {
                                                StoryCarouselCard(
                                                    story = Story(
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
                                                    ),
                                                    onClick = onClick,
                                                    apiBaseUrl = apiBaseUrl
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.background)
                                    .padding(horizontal = AraroDesignTokens.contentPaddingHorizontal)
                                    .padding(top = AraroDesignTokens.sectionSpacing)
                            ) {
                                AraroStoryFilter(
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = onCategorySelect
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = Strings.trending(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.onSurface
                                )
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(AraroDesignTokens.cardSpacing),
                                    contentPadding = PaddingValues(end = AraroDesignTokens.contentPaddingHorizontal)
                                ) {
                                    items(featuredStories) { story ->
                                        StoryCarouselCard(
                                            story = story,
                                            onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                            apiBaseUrl = apiBaseUrl
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = AraroDesignTokens.contentPaddingHorizontal,
                                        vertical = AraroDesignTokens.sectionSpacing
                                    )
                            ) {
                                Text(
                                    text = Strings.allStories(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                        itemsIndexed(displayStories) { index, story ->
                            StoryCard(
                                story = story,
                                onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                visible = true,
                                animationDelayMillis = (index % 8) * 50,
                                modifier = Modifier
                                    .padding(horizontal = AraroDesignTokens.contentPaddingHorizontal)
                                    .padding(bottom = AraroDesignTokens.cardSpacing),
                                apiBaseUrl = apiBaseUrl,
                                isFavorite = story.id in favoriteStoryIds,
                                onFavoriteClick = { onFavoriteToggle(story, story.id !in favoriteStoryIds) }
                            )
                        }
                    }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = onNewStory,
                containerColor = AraroColors.goldAccent,
                contentColor = colorScheme.onPrimary,
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AraroDesignTokens.contentPaddingHorizontal)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(Strings.newStory(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun filterStoriesByCategory(stories: List<Story>, category: String): List<Story> {
    if (category == "All") return stories
    val lower = category.lowercase()
    return stories.filter { story ->
        val theme = story.theme.lowercase()
        when {
            lower == "bedtime" -> theme.contains("night") || theme.contains("bed") || theme.contains("moon") || theme.contains("sleep")
            lower == "moral" -> theme.contains("moral") || theme.contains("clever") || theme.contains("lesson")
            lower == "animals" -> theme.contains("fox") || theme.contains("lion") || theme.contains("monkey") || theme.contains("ant") || theme.contains("rabbit") || theme.contains("crocodile") || theme.contains("donkey") || theme.contains("grasshopper")
            lower == "mythology" -> theme.contains("tenali") || theme.contains("king") || theme.contains("rama") || theme.contains("myth")
            else -> true
        }
    }
}

@Composable
fun DashboardScreenPreview() {
    com.araro.ui.theme.AraroTheme {
        DashboardScreen(
            greeting = com.araro.ui.strings.Strings.goodEvening(),
            childName = "Kavi",
            stories = com.araro.ui.data.SampleData.sampleStories(),
            selectedCategory = "All",
            categories = com.araro.ui.data.SampleData.categories,
            onCategorySelect = {},
            onStoryClick = {},
            onNewStory = {},
            onSettings = {},
            onRefresh = {},
            isRefreshing = false,
            onNavigateToCategories = {},
            onNavigateToFavorites = {},
            onNavigateToSearch = {},
            recommended = emptyList(),
            languageCode = com.araro.util.AraroConstants.DEFAULT_LANGUAGE,
            favoriteStoryIds = emptySet(),
            onRecommendedStoryClick = { _, _ -> },
            onFavoriteToggle = { _, _ -> }
        )
    }
}
