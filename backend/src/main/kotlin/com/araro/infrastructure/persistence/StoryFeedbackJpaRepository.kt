package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StoryFeedbackJpaRepository : JpaRepository<StoryFeedbackEntity, Long> {
    fun deleteByStoryIdAndStorySource(storyId: Long, storySource: String)
}
