package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface VoiceCloneAnalyticsJpaRepository : JpaRepository<VoiceCloneAnalyticsEntity, Long> {

    @Query("SELECT COUNT(e) FROM VoiceCloneAnalyticsEntity e WHERE e.eventType = :eventType AND e.createdAt >= :since")
    fun countByEventTypeAndCreatedAtAfter(
        @Param("eventType") eventType: String,
        @Param("since") since: Instant
    ): Long
}
