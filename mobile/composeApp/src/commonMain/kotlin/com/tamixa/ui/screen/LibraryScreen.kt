package com.tamixa.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.AgeBand
import com.tamixa.domain.Story
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.isFunStory
import com.tamixa.ui.isInteractivePracticeLibraryStory
import com.tamixa.ui.isLearnOrDigitalSafetyStory
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

/** [Interactive] is its own section; the other tabs are filters inside "Stories" (stories without questions). */
enum class LibraryHubTab {
    Browse,
    FunCorner,
    LearnSafety,
    Interactive,
}

fun LibraryHubTab.toAnalyticsHubKey(): String = when (this) {
    LibraryHubTab.Browse -> "browse"
    LibraryHubTab.FunCorner -> "fun"
    LibraryHubTab.LearnSafety -> "learn"
    LibraryHubTab.Interactive -> "simulator"
}

@Composable
private fun LibrarySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = Strings.searchStories(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = Strings.search(),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = Strings.clear(),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(TamixaDesignTokens.inputRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TamixaColors.deepTeal.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            cursorColor = TamixaColors.eduStoryMint,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

/** "Stories" | "Interactive stories"; under Stories, All / Fun / Be Smart filters. */
@Composable
private fun LibraryHubPicker(
    selected: LibraryHubTab,
    onSelect: (LibraryHubTab) -> Unit,
) {
    val interactive = selected == LibraryHubTab.Interactive
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryHubSegment(
                label = Strings.libraryStoriesSection(),
                selected = !interactive,
                onClick = { if (interactive) onSelect(LibraryHubTab.Browse) },
                modifier = Modifier.weight(1f),
            )
            LibraryHubSegment(
                label = Strings.libraryInteractiveSection(),
                selected = interactive,
                onClick = { onSelect(LibraryHubTab.Interactive) },
                modifier = Modifier.weight(1f),
            )
        }
        if (!interactive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    LibraryHubTab.Browse to Strings.libraryBrowseTab(),
                    LibraryHubTab.FunCorner to Strings.libraryFunCornerTab(),
                    LibraryHubTab.LearnSafety to Strings.libraryLearnSafetyTab(),
                ).forEach { (tab, label) ->
                    LibraryHubSegment(
                        label = label,
                        selected = selected == tab,
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
        }
    }
}

/** Child's age filter: All ages | 5–6 | 7–8 | 9–10. Shared by Library and Home. */
@Composable
internal fun AgeBandPicker(
    selected: AgeBand?,
    onSelect: (AgeBand?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = Strings.childAgeFilterLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (listOf<AgeBand?>(null) + AgeBand.entries).forEach { band ->
                LibraryHubSegment(
                    // Header already says "Child's age", so short labels keep every chip on one line.
                    label = if (band == null) Strings.libraryBrowseTab() else "${band.minAge}–${band.maxAge}",
                    selected = selected == band,
                    onClick = { onSelect(band) },
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryHubSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Smaller filter chip (used under "Stories"). */
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    val hubSpot = Color(0xFFE8E4DC).copy(alpha = if (selected) 0.14f else 0.09f)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) Color.White.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.22f),
        ),
        modifier = modifier.shadow(
            elevation = if (selected) 8.dp else 3.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (selected) 0.2f else 0.12f),
            spotColor = hubSpot,
        ),
    ) {
        val brush = if (selected) {
            Brush.horizontalGradient(
                colors = listOf(
                    TamixaColors.deepTeal.copy(alpha = 0.92f),
                    TamixaColors.terracotta.copy(alpha = 0.78f),
                ),
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.07f),
                ),
            )
        }
        Box(
            modifier = Modifier
                .background(brush)
                .fillMaxWidth()
                .padding(vertical = if (compact) 8.dp else 12.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                style = (if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
                    .copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/**
 * Story Library in two sections: **Stories** (no questions; All / Fun / Be Smart filters) and **Interactive stories**
 * (the child answers questions while listening).
 */
@Composable
fun LibraryScreen(
    /** Initial hub tab from deep link / dashboard shortcuts. */
    initialHubTab: LibraryHubTab = LibraryHubTab.Browse,
    cachedStories: List<Story>,
    loading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    onGenerateStory: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToShortContent: () -> Unit,
    onNavigateToProfile: () -> Unit,
    /** API origin for cover URLs in the poster grid. */
    apiBaseUrl: String? = null,
    /** Fires when the visible library lane changes (incl. initial deep link). No PII. */
    onHubTabChange: (LibraryHubTab) -> Unit = {},
    /** When true (e.g. Koin `tamixaBuildEnvironment` == dev), show a one-shot DSG E2E seed control. */
    showDevDigitalSurvivalPrepare: Boolean = false,
    devDigitalSurvivalPrepareBusy: Boolean = false,
    onPrepareDevDigitalSurvivalSeed: (() -> Unit)? = null,
    onNavigateToCrisisHelp: (() -> Unit)? = null,
    /** Child's age filter; the caller passes stories already narrowed to it. */
    ageBand: AgeBand? = null,
    onAgeBandChange: (AgeBand?) -> Unit = {},
) {
    var hubTab by rememberSaveable(initialHubTab) { mutableStateOf(initialHubTab) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    LaunchedEffect(hubTab) {
        onHubTabChange(hubTab)
    }
    
    val (interactiveStories, plainStories) = cachedStories.partition { isInteractivePracticeLibraryStory(it) }
    val filteredByHub = when (hubTab) {
        LibraryHubTab.Browse -> plainStories
        LibraryHubTab.FunCorner -> plainStories.filter { isFunStory(it) }
        LibraryHubTab.LearnSafety -> plainStories.filter { isLearnOrDigitalSafetyStory(it) }
        LibraryHubTab.Interactive -> interactiveStories
    }
    
    val displayStories = if (searchQuery.isNotBlank()) {
        filteredByHub.filter { story ->
            val title = story.title?.lowercase() ?: ""
            val theme = story.theme.lowercase()
            val category = story.category?.lowercase() ?: ""
            val query = searchQuery.lowercase()
            title.contains(query) || theme.contains(query) || category.contains(query)
        }
    } else {
        filteredByHub
    }
    
    StorySelectionScreen(
        cachedStories = displayStories,
        onGenerateStory = onGenerateStory,
        onStoryClick = onStoryClick,
        onBack = onNavigateToHome,
        title = Strings.storyLibrary(),
        loading = loading,
        loadError = loadError,
        loadErrorHint = if (loadError != null) Strings.libraryCatalogUnavailableHint() else null,
        onRetry = if (loadError != null) ({ onRetry() }) else null,
        emptyStateSubtitle = when {
            searchQuery.isNotBlank() -> Strings.noSearchResults()
            hubTab == LibraryHubTab.Browse -> Strings.generateFirstStoryPrompt()
            hubTab == LibraryHubTab.FunCorner -> Strings.funCornerEmptyHint()
            hubTab == LibraryHubTab.LearnSafety -> Strings.learnSafetyEmptyHint()
            hubTab == LibraryHubTab.Interactive -> Strings.simulatorHubEmptyHint()
            else -> Strings.generateFirstStoryPrompt()
        },
        listLayout = StorySelectionListLayout.LibraryPosterGrid,
        apiBaseUrl = apiBaseUrl,
        filterRow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LibrarySearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearch = { /* Search is reactive, no action needed */ },
                )
                AgeBandPicker(
                    selected = ageBand,
                    onSelect = onAgeBandChange,
                )
                LibraryHubPicker(
                    selected = hubTab,
                    onSelect = { hubTab = it },
                )
                if (hubTab == LibraryHubTab.Interactive) {
                    onNavigateToCrisisHelp?.let { go ->
                        Surface(
                            onClick = go,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // Stacked so long Tamil titles and the button never squeeze each other on narrow phones.
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = Strings.crisisHelpLibraryBannerTitle(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = Strings.crisisHelpLibraryBannerSubtitle(),
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = Color.White.copy(alpha = 0.88f),
                                )
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    onClick = go,
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
                                ) {
                                    Text(
                                        Strings.crisisHelpOpenDirectory(),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (showDevDigitalSurvivalPrepare) {
                    val seedHandler = onPrepareDevDigitalSurvivalSeed
                    if (seedHandler != null) {
                        TextButton(
                            onClick = seedHandler,
                            enabled = !devDigitalSurvivalPrepareBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text =
                                    if (devDigitalSurvivalPrepareBusy) {
                                        "Preparing Digital Survival seed…"
                                    } else {
                                        "Dev: prepare Digital Survival E2E seed"
                                    },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.92f),
                            )
                        }
                    }
                }
                if (searchQuery.isBlank()) {
                    Text(
                        text = Strings.dashboardSpotlightSubtitle(),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }
        },
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.Library,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile,
            )
        },
    )
}
