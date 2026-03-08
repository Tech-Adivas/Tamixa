package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface StoryTokenUsageJpaRepository : JpaRepository<StoryTokenUsageEntity, Long> {

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM StoryTokenUsageEntity u WHERE u.story.parent.id = :parentId AND u.createdAt >= :start AND u.createdAt < :end")
    fun sumTotalTokensByParentAndDateRange(parentId: Long, start: Instant, end: Instant): Long

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM StoryTokenUsageEntity u WHERE u.createdAt >= :start AND u.createdAt < :end")
    fun sumTotalTokensByDateRange(start: Instant, end: Instant): Long
}
