package com.araro.infrastructure.persistence

import com.araro.application.port.SubscriptionRepositoryPort
import com.araro.domain.PaymentProvider
import com.araro.domain.Subscription
import com.araro.domain.SubscriptionPlan
import com.araro.domain.SubscriptionStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SubscriptionRepositoryAdapter(
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : SubscriptionRepositoryPort {

    override fun save(subscription: Subscription): Subscription {
        val parent = parentJpaRepository.findById(subscription.parentId).orElseThrow {
            IllegalArgumentException("Parent not found: ${subscription.parentId}")
        }
        val now = Instant.now()
        val entity = if (subscription.id > 0) {
            subscriptionJpaRepository.findById(subscription.id).orElseThrow {
                IllegalArgumentException("Subscription not found: ${subscription.id}")
            }.apply {
                status = subscription.status
                currentPeriodEnd = subscription.currentPeriodEnd
                cancelAtPeriodEnd = subscription.cancelAtPeriodEnd
                canceledAt = subscription.canceledAt
                pastDueAt = subscription.pastDueAt
                trialUsed = subscription.trialUsed
                updatedAt = now
            }
        } else {
            SubscriptionEntity(
                id = 0,
                parent = parent,
                plan = subscription.plan,
                status = subscription.status,
                provider = subscription.provider,
                externalSubscriptionId = subscription.externalSubscriptionId,
                externalCustomerId = subscription.externalCustomerId,
                currentPeriodStart = subscription.currentPeriodStart,
                currentPeriodEnd = subscription.currentPeriodEnd,
                trialEnd = subscription.trialEnd,
                cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
                canceledAt = subscription.canceledAt,
                pastDueAt = subscription.pastDueAt,
                maxChildren = subscription.maxChildren,
                voicePremium = subscription.voicePremium,
                trialUsed = subscription.trialUsed,
                createdAt = subscription.createdAt,
                updatedAt = now
            )
        }
        val saved = subscriptionJpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): Subscription? {
        return subscriptionJpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByParentId(parentId: Long): Subscription? {
        return subscriptionJpaRepository.findByParent_Id(parentId)?.toDomain()
    }

    override fun findByProviderAndExternalId(provider: String, externalSubscriptionId: String): Subscription? {
        return subscriptionJpaRepository.findByProviderAndExternalSubscriptionId(provider, externalSubscriptionId)?.toDomain()
    }

    override fun findSubscriptionIdsByStatusIn(statuses: List<SubscriptionStatus>): List<Long> {
        return subscriptionJpaRepository.findByStatusIn(statuses).map { it.id }
    }

    override fun countByStatus(status: SubscriptionStatus): Long {
        return subscriptionJpaRepository.countByStatus(status)
    }
}

private fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    parentId = parent.id,
    plan = plan,
    status = status,
    provider = provider,
    externalSubscriptionId = externalSubscriptionId,
    externalCustomerId = externalCustomerId,
    currentPeriodStart = currentPeriodStart,
    currentPeriodEnd = currentPeriodEnd,
    trialEnd = trialEnd,
    cancelAtPeriodEnd = cancelAtPeriodEnd,
    canceledAt = canceledAt,
    pastDueAt = pastDueAt,
    maxChildren = maxChildren,
    voicePremium = voicePremium,
    trialUsed = trialUsed,
    createdAt = createdAt,
    updatedAt = updatedAt
)
