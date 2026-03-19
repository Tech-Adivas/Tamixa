package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional

interface StorySegmentJpaRepository : JpaRepository<StorySegmentEntity, Long> {
    fun findBySceneIdOrderBySegmentIndex(sceneId: Long): List<StorySegmentEntity>

    @Modifying
    @Transactional
    fun deleteBySceneId(sceneId: Long)
}
