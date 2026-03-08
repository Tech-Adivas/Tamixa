package com.araro.domain

import java.time.Instant

data class Subscription(
    val id: Long,
    val parentId: Long,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val provider: PaymentProvider,
    val externalSubscriptionId: String?,
    val externalCustomerId: String?,
    val currentPeriodStart: Instant?,
    val currentPeriodEnd: Instant?,
    val trialEnd: Instant?,
    val cancelAtPeriodEnd: Boolean,
    val canceledAt: Instant?,
    val pastDueAt: Instant?,
    val maxChildren: Int,  // for FAMILY plan
    val voicePremium: Boolean,
    val trialUsed: Boolean = false,  // Fraud prevention: block multiple trial abuse
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Server-side entitlement: never trust client. Premium/trial/grace/canceled-until-period-end get unlimited. */
    fun isEntitledToUnlimitedStories(): Boolean =
        plan != SubscriptionPlan.FREE && status in listOf(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.CANCELED)

    /** True if subscription is in a state that allows access (active, trial, grace, or past_due during retry). */
    fun isInGraceOrActive(): Boolean =
        status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.GRACE_PERIOD ||
        status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.PAST_DUE

    fun allowsVoicePremium(): Boolean =
        voicePremium && isInGraceOrActive()
}

enum class PaymentProvider {
    STRIPE,
    ZOHO
}
