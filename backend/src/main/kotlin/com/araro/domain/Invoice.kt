package com.araro.domain

import java.time.Instant

/**
 * Invoice record for financial audit; linked to subscription when applicable.
 */
data class Invoice(
    val id: Long,
    val providerInvoiceId: String,
    val parentId: Long,
    val subscriptionId: Long?,
    val amountMinor: Long,
    val currency: String,
    val status: InvoiceStatus,
    val paidAt: Instant?,
    val createdAt: Instant
)

enum class InvoiceStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
