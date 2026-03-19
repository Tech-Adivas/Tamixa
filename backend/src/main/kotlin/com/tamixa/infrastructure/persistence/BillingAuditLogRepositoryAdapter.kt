package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.BillingAuditPort
import com.tamixa.domain.BillingAuditLog
import com.tamixa.domain.BillingEventType
import com.tamixa.domain.PaymentProvider
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class BillingAuditLogRepositoryAdapter(
    private val billingAuditLogJpaRepository: BillingAuditLogJpaRepository,
    private val parentJpaRepository: ParentJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository
) : BillingAuditPort {

    override fun append(entry: BillingAuditLog): BillingAuditLog {
        val parent = parentJpaRepository.findById(entry.parentId).orElseThrow {
            IllegalArgumentException("Parent not found: ${entry.parentId}")
        }
        val subscription = entry.subscriptionId?.let {
            subscriptionJpaRepository.findById(it).orElse(null)
        }
        val entity = BillingAuditLogEntity(
            id = 0,
            parent = parent,
            subscription = subscription,
            eventType = entry.eventType,
            provider = entry.provider,
            externalId = entry.externalId,
            amountMinor = entry.amountMinor,
            currency = entry.currency,
            invoiceId = entry.invoiceId,
            details = entry.details,
            createdAt = entry.createdAt
        )
        val saved = billingAuditLogJpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun sumRevenueMinor(revenueEventTypes: List<BillingEventType>, from: Instant, to: Instant): Long {
        return billingAuditLogJpaRepository.sumAmountMinorByEventTypesAndPeriod(revenueEventTypes, from, to)
    }

    override fun countEvents(eventType: BillingEventType, from: Instant, to: Instant): Long {
        return billingAuditLogJpaRepository.countByEventTypeAndCreatedAtBetween(eventType, from, to)
    }
}

private fun BillingAuditLogEntity.toDomain(): BillingAuditLog = BillingAuditLog(
    id = id,
    parentId = parent.id,
    subscriptionId = subscription?.id,
    eventType = eventType,
    provider = provider,
    externalId = externalId,
    amountMinor = amountMinor,
    currency = currency,
    invoiceId = invoiceId,
    details = details,
    createdAt = createdAt
)
