package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.components.TamixaChildrenListeningIllustration
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.StoryCard
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    storyViewModel: StoryViewModel,
    onBack: () -> Unit,
    onStoryClick: (Story) -> Unit,
    languageCode: String = com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE,
    apiBaseUrl: String? = null,
    onFavoriteRemoved: (storyId: Long, storySource: String) -> Unit = { _, _ -> }
) {
    val favorites by storyViewModel.favorites.collectAsState()
    val favoritesLoading by storyViewModel.favoritesLoading.collectAsState()
    val favoritesError by storyViewModel.favoritesError.collectAsState()
    LaunchedEffect(languageCode) {
        storyViewModel.loadFavorites(languageCode)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.favorites(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        val colorScheme = MaterialTheme.colorScheme
        Box(modifier = Modifier.fillMaxSize()) {
            AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
            when {
                favoritesLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TamixaColors.goldAccent)
                    }
                }
                favoritesError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(TamixaDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = favoritesError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaColors.cream.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { storyViewModel.loadFavorites(languageCode) }) {
                            Text(Strings.retry(), color = TamixaColors.goldAccent)
                        }
                    }
                }
                favorites.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(TamixaDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TamixaChildrenListeningIllustration(size = 160.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = Strings.noFavoritesYet(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            color = TamixaColors.cream
                        )
                        Text(
                            text = Strings.noFavoritesHint(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TamixaColors.cream.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = Strings.tapToAddFavorites(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaColors.cream.copy(alpha = 0.85f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                        contentPadding = PaddingValues(
                            top = TamixaDesignTokens.sectionSpacing,
                            bottom = TamixaDesignTokens.screenPaddingBottomWithNav
                        ),
                        verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
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
                                        onFavoriteRemoved(story.id, if (story.parentId == 0L) "library" else "generated")
                                        storyViewModel.removeFavorite(story.id)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = Strings.favorites(),
                                        tint = TamixaColors.goldAccent
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
