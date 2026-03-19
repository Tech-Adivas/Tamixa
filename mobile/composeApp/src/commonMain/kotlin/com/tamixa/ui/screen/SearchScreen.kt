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
        val colorScheme = MaterialTheme.colorScheme
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(Strings.searchStories(), color = TamixaColors.cream.copy(alpha = 0.8f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = Strings.search(), tint = TamixaColors.cream) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TamixaDesignTokens.screenPadding, vertical = 12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearch(searchQuery)
                    }),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TamixaColors.cream,
                        unfocusedTextColor = TamixaColors.cream,
                        cursorColor = TamixaColors.goldAccent,
                        focusedBorderColor = TamixaColors.goldAccent.copy(alpha = 0.7f),
                        unfocusedBorderColor = TamixaColors.cream.copy(alpha = 0.5f),
                        focusedContainerColor = TamixaColors.inputSurface.copy(alpha = 0.9f),
                        unfocusedContainerColor = TamixaColors.inputSurface.copy(alpha = 0.7f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.inputRadius)
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
                                CircularProgressIndicator(color = TamixaColors.goldAccent)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = Strings.loading(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TamixaColors.cream.copy(alpha = 0.9f)
                                )
                            }
                        }
                        searchError != null && searchQuery.isNotBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TamixaDesignTokens.screenPadding)
                                    .semantics { contentDescription = "Error" },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = searchError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                onRetry?.let { retry ->
                                    TextButton(onClick = retry) {
                                        Text(Strings.retry(), color = TamixaColors.goldAccent)
                                    }
                                }
                            }
                        }
                        searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TamixaDesignTokens.screenPadding)
                                    .semantics { contentDescription = Strings.noSearchResults() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TamixaEmojiDisplay(emoji = "🔍", fontSize = 72.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = Strings.noSearchResults(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TamixaColors.cream
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = Strings.noSearchResultsHint(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TamixaColors.cream.copy(alpha = 0.9f)
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                                    vertical = TamixaDesignTokens.sectionSpacing
                                ),
                                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
                            ) {
                                itemsIndexed(searchResults) { index, item ->
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
