package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaEmojiDisplay
import com.tamixa.ui.components.StoryCard
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.strings.Strings
import androidx.compose.material3.MaterialTheme
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

private const val MIN_SEARCH_QUERY_CHARS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchResults: List<com.tamixa.network.SearchStoryItemDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onStoryClick: (Long, String) -> Unit,
    apiBaseUrl: String? = null,
    favoriteStoryIds: Set<Long> = emptySet(),
    onFavoriteToggle: (Long, String, Boolean) -> Unit = { _, _, _ -> },
    searchLoading: Boolean = false,
    searchError: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.search(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(
                showStars = true,
                showClouds = true,
                animateStars = true,
                ambientPresence = true
            )
            Column(modifier = Modifier.fillMaxSize()) {
                // Search input with improved styling
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { 
                        Text(
                            Strings.searchStories(), 
                            color = TamixaColors.appHeading.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = Strings.search(), 
                            tint = TamixaColors.goldAccent
                        ) 
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TamixaDesignTokens.screenPadding, vertical = 16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        if (searchQuery.length >= MIN_SEARCH_QUERY_CHARS) {
                            onSearch(searchQuery)
                        }
                    }),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TamixaColors.appHeading,
                        unfocusedTextColor = TamixaColors.appHeading,
                        cursorColor = TamixaColors.goldAccent,
                        focusedBorderColor = TamixaColors.goldAccent,
                        unfocusedBorderColor = TamixaColors.appHeading.copy(alpha = 0.3f),
                        focusedContainerColor = TamixaColors.cream.copy(alpha = 0.95f),
                        unfocusedContainerColor = TamixaColors.cream.copy(alpha = 0.9f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TamixaColors.appHeading)
                )
                
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        searchLoading && searchQuery.isNotBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TamixaDesignTokens.screenPadding)
                                    .semantics { contentDescription = Strings.loading() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TamixaEmojiDisplay(emoji = "🔍", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                CircularProgressIndicator(
                                    color = TamixaColors.goldAccent,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = Strings.searchingStories(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TamixaColors.cream
                                )
                            }
                        }
                        searchError != null && searchQuery.isNotBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TamixaDesignTokens.screenPadding)
                                    .semantics { contentDescription = Strings.accessibilityErrorState() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TamixaEmojiDisplay(emoji = "😕", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = searchError,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TamixaColors.cream.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                onRetry?.let { retry ->
                                    TextButton(
                                        onClick = retry,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    ) {
                                        Text(
                                            Strings.retry(), 
                                            color = TamixaColors.goldAccent,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                        searchResults.isEmpty() &&
                            searchQuery.isNotBlank() &&
                            searchQuery.length >= MIN_SEARCH_QUERY_CHARS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TamixaDesignTokens.screenPadding)
                                    .semantics { contentDescription = Strings.noSearchResults() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TamixaEmojiDisplay(emoji = "🔍", fontSize = 80.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = Strings.noSearchResults(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TamixaColors.cream
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = Strings.noSearchResultsHint(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TamixaColors.cream.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        searchQuery.isBlank() || searchQuery.length < MIN_SEARCH_QUERY_CHARS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TamixaDesignTokens.screenPadding)
                                    .semantics { contentDescription = Strings.searchMinCharactersHint() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TamixaEmojiDisplay(emoji = "✨", fontSize = 80.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = Strings.discoverStories(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TamixaColors.cream,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = Strings.searchWhatYouCanFindHint(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TamixaColors.cream.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                                    vertical = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
                            ) {
                                item {
                                    Text(
                                        text = "${searchResults.size} ${if (searchResults.size == 1) Strings.storyFound() else Strings.storiesFound()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TamixaColors.cream.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                itemsIndexed(searchResults, key = { _, item -> "${item.storyId}-${item.storySource}" }) { index, item ->
                                    val story = item.toStory()
                                    StoryCard(
                                        story = story,
                                        onClick = { onStoryClick(item.storyId, item.storySource) },
                                        visible = true,
                                        animationDelayMillis = (index % 8) * 50,
                                        modifier = Modifier.fillMaxWidth(),
                                        apiBaseUrl = apiBaseUrl,
                                        isFavorite = item.storyId in favoriteStoryIds,
                                        onFavoriteClick = { onFavoriteToggle(item.storyId, item.storySource, item.storyId !in favoriteStoryIds) }
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

private fun com.tamixa.network.SearchStoryItemDto.toStory(): Story = Story(
    id = storyId,
    parentId = if (storySource.lowercase() == com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY) 0L else 1L,
    childId = null,
    content = "",
    theme = title?.takeIf { it.isNotBlank() } ?: theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    title = title?.takeIf { it.isNotBlank() },
    moral = null,
    status = runCatching { StoryStatus.valueOf(status) }.getOrDefault(StoryStatus.READY),
    audioFileUrl = null,
    coverImageUrl = coverImageUrl,
    coverVideoUrl = coverVideoUrl,
    createdAt = ""
)
