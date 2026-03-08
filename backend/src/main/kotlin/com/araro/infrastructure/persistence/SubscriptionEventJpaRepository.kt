package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionEventJpaRepository : JpaRepository<SubscriptionEventEntity, Long> {

    fun findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId: Long, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<SubscriptionEventEntity>
}
