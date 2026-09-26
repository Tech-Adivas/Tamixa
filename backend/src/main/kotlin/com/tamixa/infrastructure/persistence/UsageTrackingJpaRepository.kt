package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UsageTrackingJpaRepository : JpaRepository<UsageTrackingEntity, Long> {

    fun findByParent_IdAndMonth(parentId: Long, month: String): UsageTrackingEntity?

    /**
     * Race-safe create: concurrent first requests for the same parent/month used to hit
     * uq_usage_tracking_parent_month and return 500. ON CONFLICT makes the second insert a no-op.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query(
        value = "INSERT INTO usage_tracking (parent_id, month) VALUES (:parentId, :month) " +
            "ON CONFLICT (parent_id, month) DO NOTHING",
        nativeQuery = true
    )
    fun insertIfAbsent(
        @org.springframework.data.repository.query.Param("parentId") parentId: Long,
        @org.springframework.data.repository.query.Param("month") month: String
    ): Int

    @Query(
        "SELECT COALESCE(SUM(u.storiesGenerated), 0) FROM UsageTrackingEntity u " +
        "WHERE u.month = :month AND u.parent IN (SELECT s.parent FROM SubscriptionEntity s WHERE s.plan = 'FREE')"
    )
    fun sumStoriesGeneratedForFreePlan(month: String): Long

    @Query(
        "SELECT COALESCE(SUM(u.storiesGenerated), 0) FROM UsageTrackingEntity u " +
        "WHERE u.month = :month AND u.parent IN (SELECT s.parent FROM SubscriptionEntity s WHERE s.plan != 'FREE')"
    )
    fun sumStoriesGeneratedForPaidPlan(month: String): Long
    fun deleteByParent_Id(parentId: Long)
}
