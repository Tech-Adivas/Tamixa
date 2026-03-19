package com.tamixa.ui.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.domain.Story
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaChildrenListeningIllustration
import com.tamixa.ui.components.StoryThumbnailPlaceholder
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySelectionScreen(
    cachedStories: List<Story>,
    onGenerateStory: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onBack: (() -> Unit)? = null,
    title: String = Strings.stories(),
    bottomBar: @Composable () -> Unit = {},
    loading: Boolean = false,
    loadError: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = title,
                onBack = onBack,
                useTransparentBackground = true
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = TamixaDesignTokens.screenPadding,
                        top = TamixaDesignTokens.screenPadding,
                        end = TamixaDesignTokens.screenPadding,
                        bottom = 0.dp
                    )
            ) {
            FilledTonalButton(
                onClick = onGenerateStory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = Strings.generateStory(),
                    modifier = Modifier.size(22.dp),
                    tint = TamixaColors.goldAccent
                )
                Spacer(Modifier.width(12.dp))
                Text(Strings.generateStory())
            }
            Spacer(Modifier.height(24.dp))
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TamixaColors.goldAccent)
                    }
                }
                loadError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                loadError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaColors.cream.copy(alpha = 0.9f)
                            )
                            if (onRetry != null) {
                                Spacer(Modifier.height(16.dp))
                                TextButton(onClick = onRetry) {
                                    Text(Strings.retry(), color = TamixaColors.goldAccent)
                                }
                            }
                        }
                    }
                }
                cachedStories.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            TamixaChildrenListeningIllustration(size = 180.dp)
                            Spacer(Modifier.height(20.dp))
                            Text(
                                Strings.noStoriesYet(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                ),
                                color = TamixaColors.cream
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                Strings.generateFirstStoryPrompt(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaColors.cream.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav)
                    ) {
                        items(cachedStories, key = { it.id }) { story ->
                            Card(
                                onClick = { onStoryClick(story) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = TamixaCardColors.surface()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 48.dp)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LibraryStoryThumbnail(theme = story.theme)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            story.theme,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            story.childName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun LibraryStoryThumbnail(theme: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = TamixaColors.goldAccent.copy(alpha = 0.12f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            StoryThumbnailPlaceholder(theme = theme)
        }
    }
}
