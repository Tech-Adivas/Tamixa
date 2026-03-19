package com.tamixa.application.port

import com.tamixa.domain.SubscriptionReconciliationLog

/**
 * Persists subscription reconciliation audit entries.
 * Financial safety: all mismatches logged for investigation.
 */
interface SubscriptionReconciliationPort {
    fun append(log: SubscriptionReconciliationLog)
}
