package com.tamixa.repository

import com.tamixa.domain.LibraryStoryResponse
import com.tamixa.util.TamixaConstants
import com.tamixa.domain.GenerateStoryRequest
import com.tamixa.domain.Story
import com.tamixa.domain.toStory
import com.tamixa.network.PlaybackPositionDto
import com.tamixa.network.toContinueListeningStory

interface StoryCache {
    fun getCachedStories(): List<Story>
    suspend fun cacheStory(story: Story)
}

data class StoriesPage(
    val content: List<Story>,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

/** Recent plays with hydrated story row + optional progress fraction from the server (0..1). */
data class RecentPlaybackHydrated(
    val dto: PlaybackPositionDto,
    val story: Story,
    val progressFraction: Float?
)

class StoryRepository(
    private val api: com.tamixa.network.StoryApi,
    private val cache: StoryCache? = null
) {
    suspend fun generate(request: GenerateStoryRequest): Result<Story> = runCatching {
        val story = api.generate(request)
        cache?.let { it.cacheStory(story) }
        story
    }

    fun getCachedStories(): List<Story> = cache?.getCachedStories() ?: emptyList()

    /** Fetch parent's generated stories from server (paginated). */
    suspend fun getMyStories(page: Int = 0, size: Int = TamixaConstants.MY_STORIES_PAGE_SIZE): Result<StoriesPage> = runCatching {
        val resp = api.getMyStories(page, size)
        StoriesPage(
            content = resp.content.map { it.toStory() },
            totalElements = resp.totalElements,
            totalPages = resp.totalPages,
            first = resp.first,
            last = resp.last
        )
    }

    suspend fun getLibraryStories(language: String = TamixaConstants.DEFAULT_LANGUAGE): Result<List<Story>> = runCatching {
        api.getLibraryStories(language).map(LibraryStoryResponse::toStory)
    }

    suspend fun getRecommended(childId: Long? = null, language: String = TamixaConstants.DEFAULT_LANGUAGE, limit: Int = TamixaConstants.RECOMMENDED_LIMIT): Result<List<com.tamixa.network.RecommendedStoryDto>> = runCatching {
        api.getRecommended(childId, language, limit)
    }

    suspend fun remix(storyId: Long, remixInstruction: String): Result<Story> = runCatching {
        api.remix(storyId, remixInstruction).also { cache?.cacheStory(it) }
    }

    suspend fun getLibraryStoryById(id: Long, language: String = TamixaConstants.DEFAULT_LANGUAGE): Story? =
        api.getLibraryStoryById(id, language)?.toStory()

    /** Fetch story by id and source (curated or generated). If source is generated and not found, tries curated so mixed navigation (e.g. deep link with id only) still resolves. */
    suspend fun getStoryById(id: Long, storySource: String, language: String = TamixaConstants.DEFAULT_LANGUAGE): Story? =
        when (storySource.lowercase()) {
            TamixaConstants.STORY_SOURCE_LIBRARY -> api.getLibraryStoryById(id, language)?.toStory()
            else -> api.getGeneratedStoryById(id)
                ?: api.getLibraryStoryById(id, language)?.toStory()
        }

    suspend fun getFavorites(): Result<List<com.tamixa.network.FavoriteStoryDto>> = runCatching {
        api.getFavorites()
    }

    suspend fun getRecentPlayback(limit: Int = TamixaConstants.RECENT_PLAYBACK_LIMIT): Result<List<com.tamixa.network.PlaybackPositionDto>> = runCatching {
        api.getRecentPlayback(limit)
    }

    suspend fun getRecentPlaybackHydrated(
        language: String = TamixaConstants.DEFAULT_LANGUAGE,
        limit: Int = TamixaConstants.RECENT_PLAYBACK_LIMIT
    ): Result<List<RecentPlaybackHydrated>> = runCatching {
        api.getRecentPlaybackEnriched(limit).map { e ->
            RecentPlaybackHydrated(
                dto = PlaybackPositionDto(
                    storyId = e.storyId,
                    storySource = e.storySource,
                    positionSeconds = e.positionSeconds,
                    updatedAt = e.updatedAt
                ),
                story = e.toContinueListeningStory(language),
                progressFraction = e.progress?.toFloat()?.coerceIn(0f, 1f)
            )
        }
    }

    suspend fun searchStories(q: String, language: String = TamixaConstants.DEFAULT_LANGUAGE, page: Int = 0, size: Int = TamixaConstants.SEARCH_PAGE_SIZE): Result<com.tamixa.network.SearchStoriesResponse> = runCatching {
        api.searchStories(q, language, page, size)
    }

    suspend fun addFavorite(storyId: Long, storySource: String = TamixaConstants.STORY_SOURCE_GENERATED): Result<Unit> = runCatching {
        api.addFavorite(storyId, storySource)
    }

    suspend fun removeFavorite(storyId: Long): Result<Unit> = runCatching {
        api.removeFavorite(storyId)
    }
}
