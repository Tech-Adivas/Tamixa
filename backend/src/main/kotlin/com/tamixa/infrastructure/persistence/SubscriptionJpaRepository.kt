package com.tamixa.infrastructure.persistence

import com.tamixa.domain.SubscriptionPlan
import com.tamixa.domain.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionJpaRepository : JpaRepository<SubscriptionEntity, Long> {

    fun findByParent_Id(parentId: Long): SubscriptionEntity?

    fun findByProviderAndExternalSubscriptionId(provider: String, externalId: String): SubscriptionEntity?

    fun findByParent_IdIn(parentIds: List<Long>): List<SubscriptionEntity>

    fun findByStatusIn(statuses: List<SubscriptionStatus>): List<SubscriptionEntity>

    fun countByStatus(status: SubscriptionStatus): Long

    fun countByPlanAndStatus(plan: SubscriptionPlan, status: SubscriptionStatus): Long

    fun findByPlan(plan: SubscriptionPlan, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<SubscriptionEntity>

    fun findByExternalSubscriptionIdIsNotNull(): List<SubscriptionEntity>
    fun deleteByParent_Id(parentId: Long)
}
