package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface GeneratedStorySegmentJpaRepository : JpaRepository<GeneratedStorySegmentEntity, Long> {
    fun findBySceneIdOrderBySegmentIndex(sceneId: Long): List<GeneratedStorySegmentEntity>
    fun deleteBySceneId(sceneId: Long)
}
