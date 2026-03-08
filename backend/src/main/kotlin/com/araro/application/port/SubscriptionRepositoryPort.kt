package com.araro.application.port

import com.araro.domain.Subscription
import com.araro.domain.SubscriptionStatus

interface SubscriptionRepositoryPort {

    fun save(subscription: Subscription): Subscription

    fun findById(id: Long): Subscription?

    fun findByParentId(parentId: Long): Subscription?

    fun findByProviderAndExternalId(provider: String, externalSubscriptionId: String): Subscription?

    /** For scheduled jobs: find subscription ids by status (e.g. PAST_DUE, GRACE_PERIOD, TRIAL). */
    fun findSubscriptionIdsByStatusIn(statuses: List<SubscriptionStatus>): List<Long>

    /** For metrics: count subscriptions with given status. */
    fun countByStatus(status: SubscriptionStatus): Long
}
