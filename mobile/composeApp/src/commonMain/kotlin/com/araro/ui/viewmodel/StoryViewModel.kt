package com.araro.ui.viewmodel

import com.araro.analytics.StoryPlaybackTracker
import com.araro.util.AraroConstants
import com.araro.domain.GenerateStoryRequest
import com.araro.domain.Story
import com.araro.repository.StoryRepository
import com.araro.util.AraroLog
import com.araro.ui.errorMessageForUser
import com.araro.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoryViewModel(
    private val storyRepository: StoryRepository,
    private val playbackTracker: StoryPlaybackTracker,
    private val scope: CoroutineScope,
    private val appMessageNotifier: com.araro.ui.AppMessageNotifier? = null
) {
    private val _generateState = MutableStateFlow<UiState<Story>>(UiState.Loading)
    val generateState: StateFlow<UiState<Story>> = _generateState.asStateFlow()

    private val _curatedStories = MutableStateFlow<List<Story>>(emptyList())
    val curatedStories: StateFlow<List<Story>> = _curatedStories.asStateFlow()

    private val _myStories = MutableStateFlow<List<Story>>(emptyList())
    val myStories: StateFlow<List<Story>> = _myStories.asStateFlow()

    private val _favorites = MutableStateFlow<List<Story>>(emptyList())
    val favorites: StateFlow<List<Story>> = _favorites.asStateFlow()

    private val _recentPlayback = MutableStateFlow<List<com.araro.network.PlaybackPositionDto>>(emptyList())
    val recentPlayback: StateFlow<List<com.araro.network.PlaybackPositionDto>> = _recentPlayback.asStateFlow()

    /** Recent playback with resolved Story for display. Loaded by loadRecentPlayback. */
    private val _recentPlaybackWithStories = MutableStateFlow<List<Pair<com.araro.network.PlaybackPositionDto, Story>>>(emptyList())
    val recentPlaybackWithStories: StateFlow<List<Pair<com.araro.network.PlaybackPositionDto, Story>>> = _recentPlaybackWithStories.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.araro.network.SearchStoryItemDto>>(emptyList())
    val searchResults: StateFlow<List<com.araro.network.SearchStoryItemDto>> = _searchResults.asStateFlow()

    /** Recommended stories with resolved Story for display. Loaded by loadRecommended. */
    private val _recommendedWithStories = MutableStateFlow<List<Pair<com.araro.network.RecommendedStoryDto, Story?>>>(emptyList())
    val recommendedWithStories: StateFlow<List<Pair<com.araro.network.RecommendedStoryDto, Story?>>> = _recommendedWithStories.asStateFlow()

    fun cachedStories(): List<Story> = storyRepository.getCachedStories()

    /** Combined: AI-generated (from server + cached) + curated Tamil stories. Deduped by id. */
    fun allStories(): List<Story> {
        val serverIds = _myStories.value.map { it.id }.toSet()
        val cachedNotOnServer = cachedStories().filter { it.id !in serverIds }
        val curatedIds = _curatedStories.value.map { it.id }.toSet()
        return (_myStories.value + cachedNotOnServer)
            .filter { it.id !in curatedIds }
            .distinctBy { it.id } + _curatedStories.value
    }

    fun loadMyStories(page: Int = 0, size: Int = AraroConstants.MY_STORIES_PAGE_SIZE) {
        scope.launch {
            storyRepository.getMyStories(page, size)
                .fold(
                    onSuccess = { _myStories.value = it.content },
                    onFailure = {
                        AraroLog.w("StoryViewModel", "loadMyStories failed", it)
                        _myStories.value = emptyList()
                        appMessageNotifier?.show(errorMessageForUser(it))
                    }
                )
        }
    }

    fun loadCuratedStories(language: String = AraroConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            storyRepository.getCuratedStories(language)
                .fold(
                    onSuccess = { _curatedStories.value = it },
                    onFailure = {
                        AraroLog.w("StoryViewModel", "loadCuratedStories failed", it)
                        _curatedStories.value = emptyList()
                        appMessageNotifier?.show(errorMessageForUser(it))
                    }
                )
        }
    }

    fun loadFavorites(language: String = AraroConstants.DEFAULT_LANGUAGE) {
        scope.launch {
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
                    appMessageNotifier?.show(errorMessageForUser(it))
                }
            )
        }
    }

    fun loadRecentPlayback(limit: Int = AraroConstants.RECENT_PLAYBACK_LIMIT, language: String = AraroConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            storyRepository.getRecentPlayback(limit).fold(
                onSuccess = { list ->
                    _recentPlayback.value = list
                    val pairs = list.mapNotNull { dto ->
                        storyRepository.getStoryById(dto.storyId, dto.storySource, language)
                            ?.let { Pair(dto, it) }
                    }
                    _recentPlaybackWithStories.value = pairs
                },
                onFailure = {
                    _recentPlayback.value = emptyList()
                    _recentPlaybackWithStories.value = emptyList()
                    appMessageNotifier?.show(errorMessageForUser(it))
                }
            )
        }
    }

    suspend fun fetchStoryById(
        id: Long,
        language: String = AraroConstants.DEFAULT_LANGUAGE,
        storySource: String? = null,
        forceRefresh: Boolean = false
    ): Story? {
        if (!forceRefresh) {
            val cached = cachedStories().find { it.id == id }
            if (cached != null) return cached
            val curated = _curatedStories.value.find { it.id == id }
            if (curated != null) return curated
            val myStories = _myStories.value.find { it.id == id }
            if (myStories != null) return myStories
        }
        if (storySource != null) {
            return storyRepository.getStoryById(id, storySource, language)
        }
        return storyRepository.getCuratedStoryById(id, language)
            ?: storyRepository.getStoryById(id, AraroConstants.STORY_SOURCE_GENERATED, language)
    }

    fun generateStory(request: GenerateStoryRequest) {
        scope.launch {
            _generateState.value = UiState.Loading
            storyRepository.generate(request)
                .fold(
                    onSuccess = { _generateState.value = UiState.Success(it) },
                    onFailure = {
                        AraroLog.w("StoryViewModel", "generateStory failed", it)
                        _generateState.value = UiState.Error(it.message ?: "Failed to generate story", it)
                    }
                )
        }
    }

    fun playbackTracker(): StoryPlaybackTracker = playbackTracker

    fun addFavorite(storyId: Long, storySource: String = AraroConstants.STORY_SOURCE_GENERATED, onSuccess: () -> Unit = {}) {
        scope.launch {
            storyRepository.addFavorite(storyId, storySource)
                .onSuccess { loadFavorites() }
                .onFailure { appMessageNotifier?.show(errorMessageForUser(it)) }
        }
    }

    fun removeFavorite(storyId: Long, onSuccess: () -> Unit = {}) {
        scope.launch {
            storyRepository.removeFavorite(storyId)
                .onSuccess { loadFavorites() }
                .onFailure { appMessageNotifier?.show(errorMessageForUser(it)) }
        }
    }

    fun searchStories(query: String, language: String = AraroConstants.DEFAULT_LANGUAGE, page: Int = 0, size: Int = AraroConstants.SEARCH_PAGE_SIZE) {
        scope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            storyRepository.searchStories(query, language, page, size)
                .fold(
                    onSuccess = { _searchResults.value = it.content },
                    onFailure = {
                        _searchResults.value = emptyList()
                        appMessageNotifier?.show(errorMessageForUser(it))
                    }
                )
        }
    }

    fun loadRecommended(childId: Long? = null, language: String = AraroConstants.DEFAULT_LANGUAGE, limit: Int = AraroConstants.RECOMMENDED_LIMIT) {
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
                    appMessageNotifier?.show(errorMessageForUser(it))
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
                onFailure = { appMessageNotifier?.show(errorMessageForUser(it)) }
            )
        }
    }
}
