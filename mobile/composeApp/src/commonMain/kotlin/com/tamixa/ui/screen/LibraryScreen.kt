package com.tamixa.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.tamixa.domain.Story
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.strings.Strings

/**
 * Story Library screen per MVP blueprint: browse all stories with bottom nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    cachedStories: List<Story>,
    loading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    onGenerateStory: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToShortContent: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    StorySelectionScreen(
        cachedStories = cachedStories,
        onGenerateStory = onGenerateStory,
        onStoryClick = onStoryClick,
        onBack = onNavigateToHome,
        title = Strings.storyLibrary(),
        loading = loading,
        loadError = loadError,
        onRetry = if (loadError != null) onRetry else null,
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.Library,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile
            )
        }
    )
}
