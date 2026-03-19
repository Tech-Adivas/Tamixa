package com.tamixa.application.port

import com.tamixa.domain.SubscriptionTier

interface SubscriptionTierRepositoryPort {
    fun save(tier: SubscriptionTier): SubscriptionTier
    fun findById(id: Long): SubscriptionTier?
    fun findByName(name: String): SubscriptionTier?
    fun findAll(): List<SubscriptionTier>
}
