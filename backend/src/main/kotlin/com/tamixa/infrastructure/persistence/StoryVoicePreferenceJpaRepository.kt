package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StoryVoicePreferenceJpaRepository : JpaRepository<StoryVoicePreferenceEntity, Long> {
    fun findByParent_IdAndStoryIdAndStorySource(
        parentId: Long,
        storyId: Long,
        storySource: String
    ): StoryVoicePreferenceEntity?

    fun deleteByParent_IdAndStoryIdAndStorySource(parentId: Long, storyId: Long, storySource: String)
}
