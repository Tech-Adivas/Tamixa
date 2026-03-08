package com.araro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.components.StoryCard
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    storyViewModel: StoryViewModel,
    onBack: () -> Unit,
    onStoryClick: (Story) -> Unit,
    languageCode: String = com.araro.util.AraroConstants.DEFAULT_LANGUAGE,
    apiBaseUrl: String? = null,
    onFavoriteRemoved: (storyId: Long, storySource: String) -> Unit = { _, _ -> }
) {
    val favorites by storyViewModel.favorites.collectAsState()
    LaunchedEffect(languageCode) {
        storyViewModel.loadFavorites(languageCode)
    }

    Scaffold(
        topBar = {
            com.araro.ui.components.AraroScreenTopBar(
                title = Strings.favorites(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            StarryNightBackground(showClouds = false)
            when {
                favorites.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(AraroDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AraroEmojiDisplay(emoji = "💝", fontSize = 72.sp)
                        Text(
                            text = Strings.favorites(),
                            style = MaterialTheme.typography.titleLarge,
                            color = AraroColors.cream
                        )
                        Text(
                            text = Strings.noFavoritesYet(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AraroColors.cream.copy(alpha = 0.9f)
                        )
                        Text(
                            text = Strings.tapToAddFavorites(),
                            style = MaterialTheme.typography.bodySmall,
                            color = AraroColors.cream.copy(alpha = 0.85f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = AraroDesignTokens.contentPaddingHorizontal),
                        contentPadding = PaddingValues(vertical = AraroDesignTokens.sectionSpacing),
                        verticalArrangement = Arrangement.spacedBy(AraroDesignTokens.cardSpacing)
                    ) {
                        itemsIndexed(favorites) { index, story ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                StoryCard(
                                    story = story,
                                    onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                    visible = true,
                                    animationDelayMillis = (index % 8) * 50,
                                    modifier = Modifier.fillMaxWidth(),
                                    apiBaseUrl = apiBaseUrl
                                )
                                IconButton(
                                    onClick = {
                                        onFavoriteRemoved(story.id, if (story.parentId == 0L) "curated" else "generated")
                                        storyViewModel.removeFavorite(story.id)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = Strings.favorites(),
                                        tint = AraroColors.goldAccent
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
