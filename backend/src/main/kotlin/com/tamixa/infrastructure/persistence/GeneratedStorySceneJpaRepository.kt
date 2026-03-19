package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface GeneratedStorySceneJpaRepository : JpaRepository<GeneratedStorySceneEntity, Long> {
    fun findByStoryIdAndLanguageOrderBySceneIndex(storyId: Long, language: String): List<GeneratedStorySceneEntity>
    fun deleteByStoryIdAndLanguage(storyId: Long, language: String)
}
