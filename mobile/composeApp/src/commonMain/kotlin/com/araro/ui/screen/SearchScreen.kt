package com.araro.ui.screen

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.components.StoryCard
import com.araro.ui.strings.Strings
import androidx.compose.material3.MaterialTheme
import com.araro.ui.theme.AraroDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchResults: List<com.araro.network.SearchStoryItemDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onStoryClick: (Long, String) -> Unit,
    apiBaseUrl: String? = null,
    favoriteStoryIds: Set<Long> = emptySet(),
    onFavoriteToggle: (Long, String, Boolean) -> Unit = { _, _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(Strings.searchStories(), color = colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = Strings.search(), tint = colorScheme.onSurface) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            onSearch(searchQuery)
                        }),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedBorderColor = colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                            unfocusedContainerColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back(), tint = colorScheme.onSurface)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AraroDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AraroEmojiDisplay(emoji = "🔍", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Strings.noStoriesYet(),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.tryDifferentSearch(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = AraroDesignTokens.contentPaddingHorizontal,
                            vertical = AraroDesignTokens.sectionSpacing
                        ),
                        verticalArrangement = Arrangement.spacedBy(AraroDesignTokens.cardSpacing)
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

private fun com.araro.network.SearchStoryItemDto.toStory(): Story = Story(
    id = storyId,
    parentId = if (storySource.lowercase() == com.araro.util.AraroConstants.STORY_SOURCE_CURATED) 0L else 1L,
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
