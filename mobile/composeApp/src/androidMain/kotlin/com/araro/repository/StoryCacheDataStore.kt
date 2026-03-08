package com.araro.repository

import android.content.Context
import com.araro.domain.Story
import com.araro.util.AraroConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistent story cache using SharedPreferences. Survives app restarts for offline
 * access to previously generated/cached stories.
 */
class StoryCacheDataStore(
    private val context: Context,
    private val maxStories: Int = AraroConstants.CACHE_MAX_STORIES
) : StoryCache {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override fun getCachedStories(): List<Story> {
        val str = prefs.getString(CACHED_STORIES_KEY, "[]") ?: "[]"
        return if (str.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<Story>>(str) }.getOrElse { emptyList() }
    }

    override suspend fun cacheStory(story: Story) {
        mutex.withLock {
            val current = runCatching {
                json.decodeFromString<List<Story>>(prefs.getString(CACHED_STORIES_KEY, "[]") ?: "[]")
            }.getOrElse { emptyList() }
            val updated = (listOf(story) + current.filter { it.id != story.id }).take(maxStories)
            prefs.edit().putString(CACHED_STORIES_KEY, json.encodeToString(updated)).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "araro_story_cache"
        private const val CACHED_STORIES_KEY = "cached_stories"
    }
}
