package com.araro.application.port

import com.araro.domain.SubscriptionReconciliationLog

/**
 * Persists subscription reconciliation audit entries.
 * Financial safety: all mismatches logged for investigation.
 */
interface SubscriptionReconciliationPort {
    fun append(log: SubscriptionReconciliationLog)
}
