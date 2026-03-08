package com.araro.domain

import java.time.Instant

/**
 * Audit log for subscription reconciliation: DB state vs provider (Stripe/Razorpay).
 * Financial safety: mismatches must be investigated; all billing changes are transactional.
 */
data class SubscriptionReconciliationLog(
    val id: Long,
    val subscriptionId: Long,
    val providerStatus: String,
    val dbStatus: String,
    val mismatchFlag: Boolean,
    val checkedAt: Instant
)
