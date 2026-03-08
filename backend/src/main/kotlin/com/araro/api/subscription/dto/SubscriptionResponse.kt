package com.araro.api.subscription.dto

import java.time.Instant

/**
 * Server-authoritative subscription info. Clients must not trust local state; always use this API.
 */
data class SubscriptionResponse(
    val plan: String,
    val status: String,
    val provider: String?,
    val currentPeriodEnd: Instant?,
    val trialEnd: Instant?,
    val cancelAtPeriodEnd: Boolean,
    val maxChildren: Int,
    val voicePremium: Boolean,
    val isEntitledToUnlimitedStories: Boolean
)
