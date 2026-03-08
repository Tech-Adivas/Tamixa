package com.araro.repository

import com.araro.domain.CuratedStoryResponse
import com.araro.util.AraroConstants
import com.araro.domain.GenerateStoryRequest
import com.araro.domain.StoriesPageResponse
import com.araro.domain.Story
import com.araro.domain.toStory

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

class StoryRepository(
    private val api: com.araro.network.StoryApi,
    private val cache: StoryCache? = null
) {
    suspend fun generate(request: GenerateStoryRequest): Result<Story> = runCatching {
        val story = api.generate(request)
        cache?.let { it.cacheStory(story) }
        story
    }

    fun getCachedStories(): List<Story> = cache?.getCachedStories() ?: emptyList()

    /** Fetch parent's generated stories from server (paginated). */
    suspend fun getMyStories(page: Int = 0, size: Int = AraroConstants.MY_STORIES_PAGE_SIZE): Result<StoriesPage> = runCatching {
        val resp = api.getMyStories(page, size)
        StoriesPage(
            content = resp.content.map { it.toStory() },
            totalElements = resp.totalElements,
            totalPages = resp.totalPages,
            first = resp.first,
            last = resp.last
        )
    }

    suspend fun getCuratedStories(language: String = AraroConstants.DEFAULT_LANGUAGE): Result<List<Story>> = runCatching {
        api.getCuratedStories(language).map(CuratedStoryResponse::toStory)
    }

    suspend fun getRecommended(childId: Long? = null, language: String = AraroConstants.DEFAULT_LANGUAGE, limit: Int = AraroConstants.RECOMMENDED_LIMIT): Result<List<com.araro.network.RecommendedStoryDto>> = runCatching {
        api.getRecommended(childId, language, limit)
    }

    suspend fun remix(storyId: Long, remixInstruction: String): Result<Story> = runCatching {
        api.remix(storyId, remixInstruction).also { cache?.cacheStory(it) }
    }

    suspend fun getCuratedStoryById(id: Long, language: String = AraroConstants.DEFAULT_LANGUAGE): Story? =
        api.getCuratedStoryById(id, language)?.toStory()

    /** Fetch story by id and source (curated or generated). If source is generated and not found, tries curated so mixed navigation (e.g. deep link with id only) still resolves. */
    suspend fun getStoryById(id: Long, storySource: String, language: String = AraroConstants.DEFAULT_LANGUAGE): Story? =
        when (storySource.lowercase()) {
            AraroConstants.STORY_SOURCE_CURATED -> api.getCuratedStoryById(id, language)?.toStory()
            else -> api.getGeneratedStoryById(id)
                ?: api.getCuratedStoryById(id, language)?.toStory()
        }

    suspend fun getFavorites(): Result<List<com.araro.network.FavoriteStoryDto>> = runCatching {
        api.getFavorites()
    }

    suspend fun getRecentPlayback(limit: Int = AraroConstants.RECENT_PLAYBACK_LIMIT): Result<List<com.araro.network.PlaybackPositionDto>> = runCatching {
        api.getRecentPlayback(limit)
    }

    suspend fun searchStories(q: String, language: String = AraroConstants.DEFAULT_LANGUAGE, page: Int = 0, size: Int = AraroConstants.SEARCH_PAGE_SIZE): Result<com.araro.network.SearchStoriesResponse> = runCatching {
        api.searchStories(q, language, page, size)
    }

    suspend fun addFavorite(storyId: Long, storySource: String = AraroConstants.STORY_SOURCE_GENERATED): Result<Unit> = runCatching {
        api.addFavorite(storyId, storySource)
    }

    suspend fun removeFavorite(storyId: Long): Result<Unit> = runCatching {
        api.removeFavorite(storyId)
    }
}
