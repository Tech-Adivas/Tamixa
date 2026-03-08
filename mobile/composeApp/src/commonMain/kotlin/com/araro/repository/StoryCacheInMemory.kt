package com.araro.repository

import com.araro.domain.Story
import com.araro.util.AraroConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StoryCacheInMemory(private val maxStories: Int = AraroConstants.CACHE_MAX_STORIES) : StoryCache {
    private val mutex = Mutex()
    private val list = mutableListOf<Story>()

    override fun getCachedStories(): List<Story> = list.toList()

    override suspend fun cacheStory(story: Story) {
        mutex.withLock {
            list.removeAll { it.id == story.id }
            list.add(0, story)
            if (list.size > maxStories) list.removeAt(list.lastIndex)
        }
    }
}
