package com.araro.application.port

import com.araro.domain.SubscriptionTier

interface SubscriptionTierRepositoryPort {
    fun save(tier: SubscriptionTier): SubscriptionTier
    fun findById(id: Long): SubscriptionTier?
    fun findByName(name: String): SubscriptionTier?
    fun findAll(): List<SubscriptionTier>
}
