package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubscriptionTierJpaRepository : JpaRepository<SubscriptionTierEntity, Long> {
    fun findByName(name: String): SubscriptionTierEntity?
}
