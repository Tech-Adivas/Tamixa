package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StorySceneJpaRepository : JpaRepository<StorySceneEntity, Long> {
    fun findByTranslationId(translationId: Long): StorySceneEntity?
    fun findByTranslationIdOrderBySceneIndex(translationId: Long): List<StorySceneEntity>
}
