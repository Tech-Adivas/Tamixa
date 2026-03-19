package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StoryFeedbackJpaRepository : JpaRepository<StoryFeedbackEntity, Long> {
    fun deleteByStoryIdAndStorySource(storyId: Long, storySource: String)
    fun deleteByParent_Id(parentId: Long)
}
