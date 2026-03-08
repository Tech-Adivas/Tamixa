package com.araro.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * Audit log for billing events: payments, refunds, invoices, revenue metrics.
 */
data class BillingAuditLog(
    val id: Long,
    val parentId: Long,
    val subscriptionId: Long?,
    val eventType: BillingEventType,
    val provider: PaymentProvider,
    val externalId: String?,
    val amountMinor: Long?,
    val currency: String?,
    val invoiceId: String?,
    val details: String?,
    val createdAt: Instant
) {
    fun amount(): BigDecimal? = amountMinor?.let { BigDecimal(it).divide(BigDecimal(100)) }
}

enum class BillingEventType {
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    INVOICE_CREATED,
    INVOICE_PAID,
    INVOICE_PAYMENT_FAILED,
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELED,
    SUBSCRIPTION_EXPIRED,
    TRIAL_ENDED,
    PRORATION_APPLIED
}
