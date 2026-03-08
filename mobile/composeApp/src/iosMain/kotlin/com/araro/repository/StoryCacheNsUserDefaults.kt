package com.araro.repository

import com.araro.domain.Story
import com.araro.util.AraroConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * Persistent story cache using NSUserDefaults. Survives app restarts for offline
 * access to previously generated/cached stories on iOS.
 */
class StoryCacheNsUserDefaults(
    private val maxStories: Int = AraroConstants.CACHE_MAX_STORIES
) : StoryCache {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override fun getCachedStories(): List<Story> {
        val str = defaults.stringForKey(CACHED_STORIES_KEY) ?: "[]"
        return if (str.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<Story>>(str) }.getOrElse { emptyList() }
    }

    override suspend fun cacheStory(story: Story) {
        mutex.withLock {
            val current = runCatching {
                json.decodeFromString<List<Story>>(
                    defaults.stringForKey(CACHED_STORIES_KEY) ?: "[]"
                )
            }.getOrElse { emptyList() }
            val updated = (listOf(story) + current.filter { it.id != story.id }).take(maxStories)
            defaults.setObject(json.encodeToString(updated), forKey = CACHED_STORIES_KEY)
            defaults.synchronize()
        }
    }

    companion object {
        private const val CACHED_STORIES_KEY = "araro_cached_stories"
    }
}
