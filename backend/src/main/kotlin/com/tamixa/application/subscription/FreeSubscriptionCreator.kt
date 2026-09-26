package com.tamixa.application.subscription

import com.tamixa.application.port.SubscriptionRepositoryPort
import com.tamixa.domain.PaymentProvider
import com.tamixa.domain.Subscription
import com.tamixa.domain.SubscriptionPlan
import com.tamixa.domain.SubscriptionStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Inserts the default FREE subscription row in its own transaction.
 *
 * Runs with REQUIRES_NEW so that a unique-key clash on `uq_subscriptions_parent_id` (two concurrent
 * first requests for the same parent, e.g. web loading `/subscription` and `/subscription/usage` together)
 * rolls back only this insert, not the caller's transaction. The caller then re-reads the winner's row.
 */
@Component
class FreeSubscriptionCreator(
    private val subscriptionRepository: SubscriptionRepositoryPort
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun insertFree(parentId: Long): Subscription {
        val now = Instant.now()
        return subscriptionRepository.save(
            Subscription(
                id = 0,
                parentId = parentId,
                plan = SubscriptionPlan.FREE,
                status = SubscriptionStatus.FREE,
                provider = PaymentProvider.STRIPE,
                externalSubscriptionId = null,
                externalCustomerId = null,
                currentPeriodStart = now,
                currentPeriodEnd = null,
                trialEnd = null,
                cancelAtPeriodEnd = false,
                canceledAt = null,
                pastDueAt = null,
                maxChildren = 1,
                voicePremium = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
