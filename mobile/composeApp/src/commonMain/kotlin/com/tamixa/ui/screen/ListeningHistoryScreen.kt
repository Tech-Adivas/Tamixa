package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.domain.Story
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.StoryThumbnailPlaceholder
import com.tamixa.ui.components.TamixaChildrenListeningIllustration
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.util.TamixaConstants

data class ListeningHistoryItem(
    val story: Story,
    val positionSeconds: Int,
    val storySource: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningHistoryScreen(
    storyViewModel: com.tamixa.ui.viewmodel.StoryViewModel,
    languageCode: String = TamixaConstants.DEFAULT_LANGUAGE,
    onBack: () -> Unit,
    onStoryClick: (Long, String) -> Unit
) {
    val recentPlaybackWithStories by storyViewModel.recentPlaybackWithStories.collectAsState()
    val recentPlaybackLoading by storyViewModel.recentPlaybackLoading.collectAsState()
    val recentPlaybackError by storyViewModel.recentPlaybackError.collectAsState()
    LaunchedEffect(languageCode) {
        storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, languageCode)
    }
    val items = recentPlaybackWithStories.map { row ->
        ListeningHistoryItem(
            story = row.story,
            positionSeconds = row.dto.positionSeconds,
            storySource = row.dto.storySource
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.listeningHistory(),
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
            when {
                recentPlaybackLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TamixaColors.goldAccent)
                    }
                }
                recentPlaybackError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(TamixaDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = recentPlaybackError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaColors.cream.copy(alpha = 0.92f)
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, languageCode) }) {
                            Text(Strings.retry(), color = TamixaColors.goldAccent)
                        }
                    }
                }
                items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(TamixaDesignTokens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TamixaChildrenListeningIllustration(size = 160.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = Strings.noListeningHistoryYet(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            color = TamixaColors.cream
                        )
                        Text(
                            text = Strings.noListeningHistoryHint(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TamixaColors.cream.copy(alpha = 0.9f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = TamixaDesignTokens.screenPadding),
                        contentPadding = PaddingValues(
                            top = TamixaDesignTokens.sectionSpacing,
                            bottom = TamixaDesignTokens.screenPaddingBottomWithNav
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(items) { _, item ->
                            Card(
                                onClick = { onStoryClick(item.story.id, item.storySource) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.cardRadius),
                                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation),
                                colors = TamixaCardColors.surface()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(TamixaDesignTokens.cardContentPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp, 48.dp)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StoryThumbnailPlaceholder(theme = item.story.theme)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.story.theme,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TamixaContentColors.cardPrimary()
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val min = item.positionSeconds / 60
                                        val sec = item.positionSeconds % 60
                                        Text(
                                            Strings.continueFrom(min, sec),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TamixaContentColors.cardSecondary()
                                        )
                                    }
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = Strings.play(),
                                        tint = TamixaColors.goldAccent,
                                        modifier = Modifier.size(24.dp)
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
