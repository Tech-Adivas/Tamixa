package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.SubscriptionEventPort
import com.tamixa.domain.PaymentProvider
import com.tamixa.domain.SubscriptionEvent
import com.tamixa.domain.SubscriptionStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class SubscriptionEventRepositoryAdapter(
    private val subscriptionEventJpaRepository: SubscriptionEventJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository
) : SubscriptionEventPort {

    override fun append(event: SubscriptionEvent): SubscriptionEvent {
        val subscription = subscriptionJpaRepository.findById(event.subscriptionId).orElseThrow {
            IllegalArgumentException("Subscription not found: ${event.subscriptionId}")
        }
        val entity = SubscriptionEventEntity(
            id = 0,
            subscription = subscription,
            eventType = event.eventType,
            previousStatus = event.previousStatus,
            newStatus = event.newStatus,
            reason = event.reason,
            provider = event.provider,
            externalEventId = event.externalEventId,
            payload = event.payload,
            createdAt = event.createdAt
        )
        val saved = subscriptionEventJpaRepository.save(entity)
        return saved.toDomain()
    }
}

private fun SubscriptionEventEntity.toDomain(): SubscriptionEvent = SubscriptionEvent(
    id = id,
    subscriptionId = subscription.id,
    eventType = eventType,
    previousStatus = previousStatus,
    newStatus = newStatus,
    reason = reason,
    provider = provider,
    externalEventId = externalEventId,
    payload = payload,
    createdAt = createdAt
)
