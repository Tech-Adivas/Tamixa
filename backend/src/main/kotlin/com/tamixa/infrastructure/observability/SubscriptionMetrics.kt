package com.tamixa.infrastructure.observability

import com.tamixa.application.port.BillingAuditPort
import com.tamixa.application.port.SubscriptionRepositoryPort
import com.tamixa.application.subscription.RevenueMetricsService
import com.tamixa.domain.BillingEventType
import com.tamixa.domain.SubscriptionStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Subscription and billing metrics for observability. Exposed via /actuator/prometheus.
 * Financial safety: all values derived from server-side data only.
 */
@Component
class SubscriptionMetrics(
    private val registry: MeterRegistry,
    private val subscriptionRepository: SubscriptionRepositoryPort,
    private val billingAuditPort: BillingAuditPort,
    private val revenueMetricsService: RevenueMetricsService
) {

    init {
        registry.gauge("subscription.active_subscriptions_count", this) { it.activeSubscriptionsCount() }
        registry.gauge("subscription.monthly_recurring_revenue_minor", this) { it.monthlyRecurringRevenueMinor() }
        registry.gauge("subscription.payment_failure_rate", this) { it.paymentFailureRate() }
        registry.gauge("subscription.trial_subscriptions_count", this) { it.trialSubscriptionsCount() }
        registry.gauge("subscription.churn_expired_count_30d", this) { it.expiredCountLast30Days() }
        registry.gauge("subscription.monthly_recurring_revenue", this) { it.monthlyRecurringRevenue() }
        registry.gauge("subscription.churn_percentage", this) { it.churnPercentage() }
        registry.gauge("subscription.trial_conversion_rate", this) { it.trialConversionRate() }
    }

    private val freeLimitHits = registry.counter("subscription.free_limit_hits")
    private val suspiciousActivityCount = registry.counter("subscription.suspicious_activity_count")

    fun recordFreeLimitHit() = freeLimitHits.increment()
    fun recordSuspiciousActivity() = suspiciousActivityCount.increment()

    private fun activeSubscriptionsCount(): Double {
        return subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE).toDouble()
    }

    private fun monthlyRecurringRevenueMinor(): Double {
        val now = YearMonth.now(ZoneOffset.UTC)
        val start = now.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = now.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        return billingAuditPort.sumRevenueMinor(
            listOf(BillingEventType.PAYMENT_SUCCEEDED, BillingEventType.INVOICE_PAID),
            start,
            end
        ).toDouble()
    }

    private fun paymentFailureRate(): Double {
        val end = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)
        val failed = billingAuditPort.countEvents(BillingEventType.PAYMENT_FAILED, start, end).toDouble()
        val succeeded = billingAuditPort.countEvents(BillingEventType.PAYMENT_SUCCEEDED, start, end).toDouble()
        val total = failed + succeeded
        return if (total > 0) failed / total else 0.0
    }

    private fun trialSubscriptionsCount(): Double {
        return subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL).toDouble()
    }

    private fun expiredCountLast30Days(): Double {
        val end = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)
        return billingAuditPort.countEvents(BillingEventType.SUBSCRIPTION_EXPIRED, start, end).toDouble()
    }

    private fun monthlyRecurringRevenue(): Double {
        val now = YearMonth.now(ZoneOffset.UTC)
        val start = now.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = now.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        return revenueMetricsService.revenueMinorFromTo(start, end).toDouble() / 100.0
    }

    private fun churnPercentage(): Double {
        val end = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)
        val expired = billingAuditPort.countEvents(BillingEventType.SUBSCRIPTION_EXPIRED, start, end)
        val active = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)
        val total = active + expired
        return if (total > 0) (expired.toDouble() / total) * 100 else 0.0
    }

    private fun trialConversionRate(): Double {
        val end = Instant.now()
        val start = end.minus(90, ChronoUnit.DAYS)
        val converted = billingAuditPort.countEvents(BillingEventType.PAYMENT_SUCCEEDED, start, end)
        val trialCount = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL)
        val total = trialCount + converted
        return if (total > 0) (converted.toDouble() / total) * 100 else 0.0
    }
}
