package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteStoryJpaRepository : JpaRepository<FavoriteStoryEntity, Long> {
    fun findByParent_Id(parentId: Long): List<FavoriteStoryEntity>
    fun findByParent_IdAndStoryId(parentId: Long, storyId: Long): FavoriteStoryEntity?
    fun existsByParent_IdAndStoryId(parentId: Long, storyId: Long): Boolean
    fun deleteByParent_IdAndStoryId(parentId: Long, storyId: Long): Int
    fun deleteByStoryIdAndStorySource(storyId: Long, storySource: String)
    fun deleteByParent_Id(parentId: Long)
}
