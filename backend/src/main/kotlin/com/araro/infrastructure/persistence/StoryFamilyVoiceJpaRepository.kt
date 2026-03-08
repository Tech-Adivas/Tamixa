package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StoryFamilyVoiceJpaRepository : JpaRepository<StoryFamilyVoiceEntity, Long> {
    fun findByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): StoryFamilyVoiceEntity?
    fun existsByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): Boolean
    fun deleteByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String)
    fun findByStoryId(storyId: Long): List<StoryFamilyVoiceEntity>
    fun deleteByStoryId(storyId: Long)
}
