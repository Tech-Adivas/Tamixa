package com.tamixa.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.Instant

/**
 * Daily revenue metrics snapshot for MRR, churn, conversion tracking.
 * Financial safety: computed server-side from billing audit; never from client.
 */
data class RevenueSnapshot(
    val id: Long,
    val snapshotDate: LocalDate,
    val mrrMinor: Long,
    val activeSubscriptions: Int,
    val churnRate: BigDecimal,
    val trialConversionRate: BigDecimal,
    val arpuMinor: Long,
    val storiesFree: Int,
    val storiesPremium: Int,
    val createdAt: Instant
) {
    fun mrr(): BigDecimal = BigDecimal(mrrMinor).divide(BigDecimal(100))
    fun arpu(): BigDecimal = BigDecimal(arpuMinor).divide(BigDecimal(100))
}
