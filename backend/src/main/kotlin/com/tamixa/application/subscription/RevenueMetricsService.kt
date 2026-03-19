package com.tamixa.application.subscription

import com.tamixa.application.port.BillingAuditPort
import com.tamixa.application.port.RevenueSnapshotPort
import com.tamixa.application.port.SubscriptionRepositoryPort
import com.tamixa.application.port.UsageMetricsPort
import com.tamixa.domain.BillingEventType
import com.tamixa.domain.RevenueSnapshot
import com.tamixa.domain.SubscriptionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Revenue metrics from billing audit log. Uses server-side data only (no client trust).
 * Computes MRR, churn, trial conversion, ARPU, story generation per plan.
 */
@Service
class RevenueMetricsService(
    private val billingAuditPort: BillingAuditPort,
    private val subscriptionRepository: SubscriptionRepositoryPort,
    private val revenueSnapshotPort: RevenueSnapshotPort,
    private val usageMetricsPort: UsageMetricsPort
) {

    private val revenueEventTypes = listOf(
        BillingEventType.PAYMENT_SUCCEEDED,
        BillingEventType.INVOICE_PAID
    )

    fun revenueMinorFromTo(from: Instant, to: Instant): Long {
        return billingAuditPort.sumRevenueMinor(revenueEventTypes, from, to)
    }

    fun revenueFromTo(from: Instant, to: Instant): BigDecimal {
        val minor = revenueMinorFromTo(from, to)
        return BigDecimal(minor).divide(BigDecimal(100))
    }

    fun revenueForMonth(yearMonth: YearMonth): BigDecimal {
        val start = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        return revenueFromTo(start, end)
    }

    /**
     * Compute and persist daily revenue snapshot for intelligence dashboards.
     * Financial safety: all metrics derived from server-side data; transactional.
     */
    @Transactional
    fun computeAndSaveDailySnapshot(date: LocalDate): RevenueSnapshot {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val monthStart = date.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val monthEnd = date.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val mrrMinor = billingAuditPort.sumRevenueMinor(revenueEventTypes, monthStart, monthEnd)
        val activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE).toInt()
        val expired30d = billingAuditPort.countEvents(
            BillingEventType.SUBSCRIPTION_EXPIRED,
            date.minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant(),
            end
        )
        val active30dAgo = activeSubscriptions + expired30d  // Approximate prior base
        val churnRate = if (active30dAgo > 0) {
            BigDecimal(expired30d).divide(BigDecimal(active30dAgo), 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val trialCount = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL)
        val trialConverted = billingAuditPort.countEvents(
            BillingEventType.PAYMENT_SUCCEEDED,
            date.minusDays(90).atStartOfDay(ZoneOffset.UTC).toInstant(),
            end
        )
        val trialConversionRate = if (trialCount > 0) {
            BigDecimal(trialConverted).divide(BigDecimal(trialCount), 4, RoundingMode.HALF_UP).min(BigDecimal.ONE)
        } else BigDecimal.ZERO
        val arpuMinor = if (activeSubscriptions > 0) mrrMinor / activeSubscriptions else 0L
        val monthStr = date.year.toString() + "-" + date.monthValue.toString().padStart(2, '0')
        val storiesFree = usageMetricsPort.sumStoriesForFreePlan(monthStr).toInt()
        val storiesPremium = usageMetricsPort.sumStoriesForPaidPlan(monthStr).toInt()

        val snapshot = RevenueSnapshot(
            id = 0,
            snapshotDate = date,
            mrrMinor = mrrMinor,
            activeSubscriptions = activeSubscriptions,
            churnRate = churnRate,
            trialConversionRate = trialConversionRate,
            arpuMinor = arpuMinor,
            storiesFree = storiesFree,
            storiesPremium = storiesPremium,
            createdAt = Instant.now()
        )
        return revenueSnapshotPort.save(snapshot)
    }
}
