package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UsageTrackingJpaRepository : JpaRepository<UsageTrackingEntity, Long> {

    fun findByParent_IdAndMonth(parentId: Long, month: String): UsageTrackingEntity?

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
