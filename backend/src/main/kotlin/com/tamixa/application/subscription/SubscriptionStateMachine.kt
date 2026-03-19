package com.tamixa.application.subscription

import com.tamixa.domain.SubscriptionStatus

/**
 * Centralized subscription state machine. All transitions must go through SubscriptionService
 * and be validated here. No invalid transitions allowed — financial correctness depends on this.
 */
object SubscriptionStateMachine {

    private val allowedTransitions: Map<SubscriptionStatus, Set<SubscriptionStatus>> = mapOf(
        SubscriptionStatus.FREE to setOf(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE),
        SubscriptionStatus.TRIAL to setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELED),
        SubscriptionStatus.ACTIVE to setOf(SubscriptionStatus.PAST_DUE, SubscriptionStatus.CANCELED, SubscriptionStatus.EXPIRED, SubscriptionStatus.PAYMENT_FAILED),
        SubscriptionStatus.PAST_DUE to setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.EXPIRED),
        SubscriptionStatus.GRACE_PERIOD to setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED),
        SubscriptionStatus.CANCELED to setOf(SubscriptionStatus.EXPIRED),
        SubscriptionStatus.EXPIRED to emptySet(),
        SubscriptionStatus.PAYMENT_FAILED to setOf(SubscriptionStatus.PAST_DUE, SubscriptionStatus.ACTIVE)
    )

    /** Returns true only if transition from -> to is allowed. Same state is allowed (no-op). */
    fun canTransition(from: SubscriptionStatus, to: SubscriptionStatus): Boolean {
        if (from == to) return true
        return allowedTransitions[from]?.contains(to) == true
    }
}
