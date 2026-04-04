package com.tamixa.ui.viewmodel

import com.tamixa.analytics.StoryPlaybackTracker
import com.tamixa.util.TamixaConstants
import com.tamixa.domain.GenerateStoryRequest
import com.tamixa.domain.GenerationTopicResponse
import com.tamixa.domain.Story
import com.tamixa.repository.RecentPlaybackHydrated
import com.tamixa.repository.StoryRepository
import com.tamixa.util.TamixaLog
import com.tamixa.ui.errorMessageForUser
import com.tamixa.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoryViewModel(
    private val storyRepository: StoryRepository,
    private val playbackTracker: StoryPlaybackTracker,
    private val scope: CoroutineScope,
    private val appMessageNotifier: com.tamixa.ui.AppMessageNotifier? = null
) {
    private val _generateState = MutableStateFlow<UiState<Story>>(UiState.Idle)
    val generateState: StateFlow<UiState<Story>> = _generateState.asStateFlow()

    fun clearGenerateState() {
        _generateState.value = UiState.Idle
    }

    private val _libraryStories = MutableStateFlow<List<Story>>(emptyList())
    val libraryStories: StateFlow<List<Story>> = _libraryStories.asStateFlow()

    private val _generationTopics = MutableStateFlow<List<GenerationTopicResponse>>(emptyList())
    val generationTopics: StateFlow<List<GenerationTopicResponse>> = _generationTopics.asStateFlow()

    private val _myStories = MutableStateFlow<List<Story>>(emptyList())
    val myStories: StateFlow<List<Story>> = _myStories.asStateFlow()

    private val _favorites = MutableStateFlow<List<Story>>(emptyList())
    val favorites: StateFlow<List<Story>> = _favorites.asStateFlow()

    private val _recentPlayback = MutableStateFlow<List<com.tamixa.network.PlaybackPositionDto>>(emptyList())
    val recentPlayback: StateFlow<List<com.tamixa.network.PlaybackPositionDto>> = _recentPlayback.asStateFlow()

    /** Recent playback with hydrated story row + server progress. Loaded by loadRecentPlayback / refreshDashboard. */
    private val _recentPlaybackWithStories = MutableStateFlow<List<RecentPlaybackHydrated>>(emptyList())
    val recentPlaybackWithStories: StateFlow<List<RecentPlaybackHydrated>> = _recentPlaybackWithStories.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.tamixa.network.SearchStoryItemDto>>(emptyList())
    val searchResults: StateFlow<List<com.tamixa.network.SearchStoryItemDto>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    /** Recommended stories with resolved Story for display. Loaded by loadRecommended. */
    private val _recommendedWithStories = MutableStateFlow<List<Pair<com.tamixa.network.RecommendedStoryDto, Story?>>>(emptyList())
    val recommendedWithStories: StateFlow<List<Pair<com.tamixa.network.RecommendedStoryDto, Story?>>> = _recommendedWithStories.asStateFlow()

    private val _dashboardRefreshing = MutableStateFlow(false)
    val dashboardRefreshing: StateFlow<Boolean> = _dashboardRefreshing.asStateFlow()

    private val _libraryLoading = MutableStateFlow(false)
    val libraryLoading: StateFlow<Boolean> = _libraryLoading.asStateFlow()
    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError: StateFlow<String?> = _libraryError.asStateFlow()

    private val _recentPlaybackLoading = MutableStateFlow(false)
    val recentPlaybackLoading: StateFlow<Boolean> = _recentPlaybackLoading.asStateFlow()
    private val _recentPlaybackError = MutableStateFlow<String?>(null)
    val recentPlaybackError: StateFlow<String?> = _recentPlaybackError.asStateFlow()

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading.asStateFlow()
    private val _favoritesError = MutableStateFlow<String?>(null)
    val favoritesError: StateFlow<String?> = _favoritesError.asStateFlow()

    /** Bumped after interactive Edu choices so Profile can refetch life-skill counters without reloading my stories. */
    private val _lifeSkillCountersRefreshVersion = MutableStateFlow(0)
    val lifeSkillCountersRefreshVersion: StateFlow<Int> = _lifeSkillCountersRefreshVersion.asStateFlow()

    fun bumpLifeSkillCountersRefresh() {
        _lifeSkillCountersRefreshVersion.value = _lifeSkillCountersRefreshVersion.value + 1
    }

    fun cachedStories(): List<Story> = storyRepository.getCachedStories()

    /** First child id from the parent's generated stories (for education / reading-level routes). */
    fun firstEducationChildId(): Long? =
        _myStories.value.asSequence().mapNotNull { it.childId }.firstOrNull { it > 0L }

    /** Load library + my stories for Library screen; sets libraryLoading and libraryError. */
    fun loadLibraryScreen(language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            _libraryLoading.value = true
            _libraryError.value = null
            try {
                storyRepository.getLibraryStories(language, theme = null, learnHub = false).fold(
                    onSuccess = { _libraryStories.value = it },
                    onFailure = {
                        TamixaLog.w("StoryViewModel", "loadLibraryScreen getLibraryStories failed", it)
                        _libraryStories.value = emptyList()
                        _libraryError.value = errorMessageForUser(it)
                        appMessageNotifier?.showError()
                    }
                )
                storyRepository.getMyStories(0, TamixaConstants.MY_STORIES_PAGE_SIZE).fold(
                    onSuccess = { _myStories.value = it.content },
                    onFailure = {
                        _myStories.value = emptyList()
                        if (_libraryError.value == null) _libraryError.value = errorMessageForUser(it)
                        appMessageNotifier?.showError()
                    }
                )
            } finally {
                _libraryLoading.value = false
            }
        }
    }

    /** Single entry point for dashboard pull-to-refresh: loads library, my stories, recent, favorites, recommended in parallel. */
    fun refreshDashboard(language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            _dashboardRefreshing.value = true
            try {
                coroutineScope {
                    val lib = async {
                        storyRepository.getLibraryStories(language, theme = null, learnHub = false).fold(
                            onSuccess = { _libraryStories.value = it },
                            onFailure = {
                                TamixaLog.w("StoryViewModel", "refreshDashboard getLibraryStories failed", it)
                                _libraryStories.value = emptyList()
                                appMessageNotifier?.showError()
                            }
                        )
                    }
                    val my = async {
                        storyRepository.getMyStories(0, TamixaConstants.MY_STORIES_PAGE_SIZE).fold(
                            onSuccess = { _myStories.value = it.content },
                            onFailure = { _myStories.value = emptyList(); appMessageNotifier?.showError() }
                        )
                    }
                    val recent = async {
                        storyRepository.getRecentPlaybackHydrated(language, TamixaConstants.RECENT_PLAYBACK_LIMIT).fold(
                            onSuccess = { rows ->
                                _recentPlayback.value = rows.map { it.dto }
                                _recentPlaybackWithStories.value = rows
                            },
                            onFailure = {
                                _recentPlayback.value = emptyList()
                                _recentPlaybackWithStories.value = emptyList()
                                appMessageNotifier?.showError()
                            }
                        )
                    }
                    val fav = async {
                        storyRepository.getFavorites().fold(
                            onSuccess = { favList ->
                                val stories = favList.mapNotNull { fav -> storyRepository.getStoryById(fav.storyId, fav.storySource, language) }
                                _favorites.value = stories
                            },
                            onFailure = { _favorites.value = emptyList(); appMessageNotifier?.showError() }
                        )
                    }
                    val rec = async {
                        storyRepository.getRecommended(null, language, TamixaConstants.RECOMMENDED_LIMIT).fold(
                            onSuccess = { dtos -> _recommendedWithStories.value = dtos.map { dto -> Pair(dto, storyRepository.getStoryById(dto.storyId, dto.storySource, language)) } },
                            onFailure = { _recommendedWithStories.value = emptyList(); appMessageNotifier?.showError() }
                        )
                    }
                    lib.await(); my.await(); recent.await(); fav.await(); rec.await()
                }
            } finally {
                _dashboardRefreshing.value = false
            }
        }
    }

    /** Combined: AI-generated (from server + cached) + curated Tamil stories. Deduped by id. */
    fun allStories(): List<Story> {
        val serverIds = _myStories.value.map { it.id }.toSet()
        val cachedNotOnServer = cachedStories().filter { it.id !in serverIds }
        val curatedIds = _libraryStories.value.map { it.id }.toSet()
        return (_myStories.value + cachedNotOnServer)
            .filter { it.id !in curatedIds }
            .distinctBy { it.id } + _libraryStories.value
    }

    fun loadMyStories(page: Int = 0, size: Int = TamixaConstants.MY_STORIES_PAGE_SIZE) {
        scope.launch {
            storyRepository.getMyStories(page, size)
                .fold(
                    onSuccess = { _myStories.value = it.content },
                    onFailure = {
                        TamixaLog.w("StoryViewModel", "loadMyStories failed", it)
                        _myStories.value = emptyList()
                        appMessageNotifier?.showError()
                    }
                )
        }
    }

    fun loadLibraryStories(language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            storyRepository.getLibraryStories(language, theme = null, learnHub = false)
                .fold(
                    onSuccess = { _libraryStories.value = it },
                    onFailure = {
                        TamixaLog.w("StoryViewModel", "loadLibraryStories failed", it)
                        _libraryStories.value = emptyList()
                        appMessageNotifier?.showError()
                    }
                )
        }
    }

    fun loadGenerationTopics() {
        scope.launch {
            storyRepository.getGenerationTopics().fold(
                onSuccess = { _generationTopics.value = it },
                onFailure = {
                    TamixaLog.w("StoryViewModel", "loadGenerationTopics failed", it)
                    _generationTopics.value = emptyList()
                }
            )
        }
    }

    fun loadFavorites(language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            _favoritesLoading.value = true
            _favoritesError.value = null
            try {
                storyRepository.getFavorites().fold(
                    onSuccess = { favList ->
                        val stories = mutableListOf<Story>()
                        favList.forEach { fav ->
                            storyRepository.getStoryById(fav.storyId, fav.storySource, language)?.let { stories.add(it) }
                        }
                        _favorites.value = stories
                    },
                    onFailure = {
                        _favorites.value = emptyList()
                        _favoritesError.value = errorMessageForUser(it)
                        appMessageNotifier?.showError()
                    }
                )
            } finally {
                _favoritesLoading.value = false
            }
        }
    }

    fun loadRecentPlayback(limit: Int = TamixaConstants.RECENT_PLAYBACK_LIMIT, language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            _recentPlaybackLoading.value = true
            _recentPlaybackError.value = null
            try {
                storyRepository.getRecentPlaybackHydrated(language, limit).fold(
                    onSuccess = { rows ->
                        _recentPlayback.value = rows.map { it.dto }
                        _recentPlaybackWithStories.value = rows
                    },
                    onFailure = {
                        _recentPlayback.value = emptyList()
                        _recentPlaybackWithStories.value = emptyList()
                        _recentPlaybackError.value = errorMessageForUser(it)
                        appMessageNotifier?.showError()
                    }
                )
            } finally {
                _recentPlaybackLoading.value = false
            }
        }
    }

    suspend fun fetchStoryById(
        id: Long,
        language: String = TamixaConstants.DEFAULT_LANGUAGE,
        storySource: String? = null,
        forceRefresh: Boolean = false
    ): Story? {
        if (!forceRefresh) {
            val cached = cachedStories().find { it.id == id }
            if (cached != null) return cached
            val curated = _libraryStories.value.find { it.id == id }
            if (curated != null) return curated
            val myStories = _myStories.value.find { it.id == id }
            if (myStories != null) return myStories
        }
        if (storySource != null) {
            return storyRepository.getStoryById(id, storySource, language)
        }
        return storyRepository.getLibraryStoryById(id, language)
            ?: storyRepository.getStoryById(id, TamixaConstants.STORY_SOURCE_GENERATED, language)
    }

    fun generateStory(request: GenerateStoryRequest) {
        scope.launch {
            _generateState.value = UiState.Loading
            storyRepository.generate(request)
                .fold(
                    onSuccess = { _generateState.value = UiState.Success(it) },
                    onFailure = {
                        TamixaLog.w("StoryViewModel", "generateStory failed", it)
                        val msg = errorMessageForUser(it)
                        _generateState.value = UiState.Error(msg, it)
                        appMessageNotifier?.showError()
                    }
                )
        }
    }

    fun playbackTracker(): StoryPlaybackTracker = playbackTracker

    fun addFavorite(storyId: Long, storySource: String = TamixaConstants.STORY_SOURCE_GENERATED, onSuccess: () -> Unit = {}) {
        scope.launch {
            storyRepository.addFavorite(storyId, storySource)
                .onSuccess {
                    loadFavorites()
                    onSuccess()
                }
                .onFailure { appMessageNotifier?.showError() }
        }
    }

    fun removeFavorite(storyId: Long, onSuccess: () -> Unit = {}) {
        scope.launch {
            storyRepository.removeFavorite(storyId)
                .onSuccess {
                    loadFavorites()
                    onSuccess()
                }
                .onFailure { appMessageNotifier?.showError() }
        }
    }

    fun searchStories(query: String, language: String = TamixaConstants.DEFAULT_LANGUAGE, page: Int = 0, size: Int = TamixaConstants.SEARCH_PAGE_SIZE) {
        scope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _searchError.value = null
                return@launch
            }
            _searchLoading.value = true
            _searchError.value = null
            try {
                storyRepository.searchStories(query, language, page, size)
                    .fold(
                        onSuccess = { _searchResults.value = it.content },
                        onFailure = {
                            _searchResults.value = emptyList()
                            val msg = errorMessageForUser(it)
                            _searchError.value = msg
                            appMessageNotifier?.showError()
                        }
                    )
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun loadRecommended(childId: Long? = null, language: String = TamixaConstants.DEFAULT_LANGUAGE, limit: Int = TamixaConstants.RECOMMENDED_LIMIT) {
        scope.launch {
            storyRepository.getRecommended(childId, language, limit).fold(
                onSuccess = { dtos ->
                    val pairs = dtos.map { dto ->
                        val story = storyRepository.getStoryById(dto.storyId, dto.storySource, language)
                        Pair(dto, story)
                    }
                    _recommendedWithStories.value = pairs
                },
                onFailure = {
                    _recommendedWithStories.value = emptyList()
                    appMessageNotifier?.showError()
                }
            )
        }
    }

    fun remixStory(storyId: Long, remixInstruction: String, onSuccess: (Story) -> Unit) {
        scope.launch {
            storyRepository.remix(storyId, remixInstruction).fold(
                onSuccess = {
                    loadMyStories()
                    onSuccess(it)
                },
                onFailure = { appMessageNotifier?.showError() }
            )
        }
    }
}
