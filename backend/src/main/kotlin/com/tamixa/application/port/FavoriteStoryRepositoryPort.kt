package com.tamixa.application.port

import com.tamixa.domain.FavoriteStory

interface FavoriteStoryRepositoryPort {
    fun findByParentId(parentId: Long): List<FavoriteStory>
    fun add(parentId: Long, storyId: Long, storySource: String): FavoriteStory
    fun remove(parentId: Long, storyId: Long)
    fun isFavorite(parentId: Long, storyId: Long): Boolean
}
