package com.araro.domain

import java.time.Instant

/**
 * Immutable event record for subscription state changes (financial audit trail).
 * reason documents why the transition occurred (e.g. "grace_period_expired", "payment_failed").
 */
data class SubscriptionEvent(
    val id: Long,
    val subscriptionId: Long,
    val eventType: String,
    val previousStatus: SubscriptionStatus?,
    val newStatus: SubscriptionStatus?,
    val reason: String?,
    val provider: PaymentProvider,
    val externalEventId: String?,
    val payload: String?,
    val createdAt: Instant
)
