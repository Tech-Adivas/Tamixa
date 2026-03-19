package com.tamixa.application.port

import com.tamixa.domain.BillingAuditLog
import com.tamixa.domain.BillingEventType
import java.time.Instant

interface BillingAuditPort {

    fun append(entry: BillingAuditLog): BillingAuditLog

    fun sumRevenueMinor(revenueEventTypes: List<BillingEventType>, from: Instant, to: Instant): Long

    /** For metrics: count events by type in period. */
    fun countEvents(eventType: BillingEventType, from: Instant, to: Instant): Long
}
