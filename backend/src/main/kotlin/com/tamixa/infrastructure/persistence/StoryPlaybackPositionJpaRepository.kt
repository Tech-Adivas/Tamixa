package com.tamixa.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface StoryPlaybackPositionJpaRepository : JpaRepository<StoryPlaybackPositionEntity, Long> {

    fun findByParent_IdAndStoryIdAndStorySource(
        parentId: Long,
        storyId: Long,
        storySource: String
    ): StoryPlaybackPositionEntity?

    fun findByParent_IdOrderByUpdatedAtDesc(parentId: Long, pageable: Pageable): List<StoryPlaybackPositionEntity>

    fun deleteByStoryIdAndStorySource(storyId: Long, storySource: String)

    fun deleteByParent_Id(parentId: Long)
}
