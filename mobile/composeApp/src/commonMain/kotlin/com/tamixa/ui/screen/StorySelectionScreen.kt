package com.tamixa.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.zIndex
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaStarfieldSection
import com.tamixa.ui.components.StoryCoverImage
import com.tamixa.ui.components.StoryThumbnailPlaceholder
import com.tamixa.ui.components.TamixaChildrenListeningIllustration
import com.tamixa.ui.isFunStory
import com.tamixa.ui.isInteractivePracticeLibraryStory
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients
import kotlin.math.roundToInt

/** Row list (categories / combined picker) vs poster grid (library). */
enum class StorySelectionListLayout {
    CompactRows,
    LibraryPosterGrid,
}

private val LibraryPosterRadius = 18.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryGenerateCta(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = TamixaDesignTokens.buttonShadowElevation,
                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                ambientColor = Color(0xFF2C2520).copy(alpha = 0.22f),
                spotColor = TamixaColors.goldAccent.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(TamixaDesignTokens.buttonRadius)),
    ) {
        Row(
            modifier = Modifier
                .background(TamixaGradients.yellowOrangeButtonBrush())
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = Strings.generateStory(),
                modifier = Modifier.size(22.dp),
                tint = Color(0xFF2A1A0A),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = Strings.generateStory(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2A1A0A),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryPosterCard(
    story: Story,
    apiBaseUrl: String?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(LibraryPosterRadius)
    val ready = story.status == StoryStatus.READY
    val titleText = story.title?.takeIf { it.isNotBlank() } ?: story.theme
    val mins = story.readingTimeMinutes.roundToInt().coerceAtLeast(1)
    val meta = buildString {
        story.category?.trim()?.takeIf { it.isNotBlank() }?.let {
            append(it)
            append(" · ")
        }
        append(Strings.minutesShort(mins))
    }
    Card(
        onClick = { if (ready) onClick() },
        enabled = ready,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StoryCoverImage(
                story = story,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                apiBaseUrl = apiBaseUrl,
            )
            if (!ready) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.42f to Color.Transparent,
                                0.72f to Color.Black.copy(alpha = 0.55f),
                                1f to Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )
            if (isFunStory(story)) {
                Text(
                    text = Strings.storyFunCornerBadge(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .zIndex(2f)
                        .shadow(
                            4.dp,
                            RoundedCornerShape(10.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.08f),
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TamixaColors.terracotta,
                                    TamixaColors.deepTeal.copy(alpha = 0.88f),
                                ),
                            ),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = Color.White,
                )
            }
            if (isInteractivePracticeLibraryStory(story)) {
                Text(
                    text = Strings.libraryInteractivePosterBadge(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .zIndex(2f)
                        .shadow(
                            4.dp,
                            RoundedCornerShape(10.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.08f),
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TamixaColors.deepTeal,
                                    TamixaColors.goldAccent.copy(alpha = 0.92f),
                                ),
                            ),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LibraryPosterSkeletonCard() {
    val shape = RoundedCornerShape(LibraryPosterRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape),
    )
}

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
    onRetry: (() -> Unit)? = null,
    /** Optional row below the generate button (e.g. Library browse vs fun corner). */
    filterRow: (@Composable () -> Unit)? = null,
    /** When the list is empty and not loading, shown under the title (default: generate-first prompt). */
    emptyStateSubtitle: String = Strings.generateFirstStoryPrompt(),
    listLayout: StorySelectionListLayout = StorySelectionListLayout.CompactRows,
    /** Resolved API origin for library cover URLs (library poster grid). */
    apiBaseUrl: String? = null,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = title,
                onBack = onBack,
                useTransparentBackground = true,
            )
        },
        bottomBar = bottomBar,
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
                        bottom = 0.dp,
                    ),
            ) {
                when (listLayout) {
                    StorySelectionListLayout.LibraryPosterGrid -> {
                        TamixaStarfieldSection {
                            LibraryGenerateCta(onClick = onGenerateStory)
                            if (filterRow != null) {
                                filterRow()
                            }
                        }
                    }
                    StorySelectionListLayout.CompactRows -> {
                        TamixaStarfieldSection {
                            FilledTonalButton(
                                onClick = onGenerateStory,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = Strings.generateStory(),
                                    modifier = Modifier.size(22.dp),
                                    tint = TamixaColors.goldAccent,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(Strings.generateStory())
                            }
                            if (filterRow != null) {
                                filterRow()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                when {
                    loading -> {
                        when (listLayout) {
                            StorySelectionListLayout.LibraryPosterGrid -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                                ) {
                                    items(6) {
                                        LibraryPosterSkeletonCard()
                                    }
                                }
                            }
                            StorySelectionListLayout.CompactRows -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = TamixaColors.goldAccent)
                                }
                            }
                        }
                    }
                    loadError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Text(
                                    loadError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TamixaColors.cream.copy(alpha = 0.9f),
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
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                TamixaChildrenListeningIllustration(size = 180.dp)
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    Strings.noStoriesYet(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TamixaColors.cream,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    emptyStateSubtitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TamixaColors.cream.copy(alpha = 0.88f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    else -> {
                        when (listLayout) {
                            StorySelectionListLayout.LibraryPosterGrid -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                                ) {
                                    items(cachedStories, key = { it.id }) { story ->
                                        LibraryPosterCard(
                                            story = story,
                                            apiBaseUrl = apiBaseUrl,
                                            onClick = { onStoryClick(story) },
                                        )
                                    }
                                }
                            }
                            StorySelectionListLayout.CompactRows -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                                ) {
                                    items(cachedStories, key = { it.id }) { story ->
                                        val headline = story.title?.takeIf { it.isNotBlank() } ?: story.theme
                                        val sub = buildString {
                                            story.category?.trim()?.takeIf { it.isNotBlank() }?.let {
                                                append(it)
                                                append(" · ")
                                            }
                                            if (isEmpty()) {
                                                story.childName.takeIf { it.isNotBlank() }?.let { append(it) }
                                            }
                                            if (isEmpty()) {
                                                append(story.wordCount)
                                                append(" words")
                                            }
                                        }
                                        Card(
                                            onClick = { onStoryClick(story) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            colors = TamixaCardColors.surface(),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 64.dp, height = 48.dp)
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    LibraryStoryThumbnail(theme = story.theme)
                                                }
                                                Spacer(Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        headline,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        sub,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = Strings.play(),
                                                    tint = TamixaColors.goldAccent,
                                                    modifier = Modifier.size(24.dp),
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

@Composable
private fun LibraryStoryThumbnail(theme: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = TamixaColors.goldAccent.copy(alpha = 0.12f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            StoryThumbnailPlaceholder(theme = theme)
        }
    }
}
