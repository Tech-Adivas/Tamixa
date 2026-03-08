package com.araro.infrastructure.persistence

import com.araro.domain.BillingEventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface BillingAuditLogJpaRepository : JpaRepository<BillingAuditLogEntity, Long> {

    fun findByParent_IdOrderByCreatedAtDesc(parentId: Long, pageable: Pageable): Page<BillingAuditLogEntity>

    @Query(
        "SELECT COALESCE(SUM(e.amountMinor), 0) FROM BillingAuditLogEntity e " +
        "WHERE e.eventType IN :revenueTypes AND e.createdAt >= :from AND e.createdAt < :to"
    )
    fun sumAmountMinorByEventTypesAndPeriod(
        revenueTypes: List<BillingEventType>,
        from: Instant,
        to: Instant
    ): Long

    fun countByEventTypeAndCreatedAtBetween(eventType: BillingEventType, from: Instant, to: Instant): Long
}
