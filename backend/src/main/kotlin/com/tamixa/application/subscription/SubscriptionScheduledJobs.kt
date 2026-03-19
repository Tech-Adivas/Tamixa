package com.tamixa.application.subscription

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Daily jobs for subscription lifecycle: grace period expiry, trial expiry,
 * revenue metrics snapshot, and subscription reconciliation.
 * All state transitions are centralized in SubscriptionService and validated by the state machine.
 */
@Component
class SubscriptionScheduledJobs(
    private val subscriptionService: SubscriptionService,
    private val revenueMetricsService: RevenueMetricsService,
    private val subscriptionReconciliationService: SubscriptionReconciliationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Run daily at 02:00 UTC; expire subscriptions that have passed grace period. */
    @Scheduled(cron = "\${app.subscription.grace-period-cron:0 0 2 * * ?}")
    fun enforceGracePeriodExpiry() {
        val expired = subscriptionService.enforceGracePeriodExpiryForAll()
        if (expired > 0) log.info("Grace period expiry job: transitioned {} subscription(s) to EXPIRED", expired)
    }

    /** Run daily at 03:00 UTC; expire subscriptions whose trial has ended. */
    @Scheduled(cron = "\${app.subscription.trial-expiry-cron:0 0 3 * * ?}")
    fun expireEndedTrials() {
        val expired = subscriptionService.expireEndedTrials()
        if (expired > 0) log.info("Trial expiry job: transitioned {} subscription(s) to EXPIRED", expired)
    }

    /** Run daily at 04:00 UTC; compute and persist revenue metrics snapshot. */
    @Scheduled(cron = "\${app.subscription.revenue-snapshot-cron:0 0 4 * * ?}")
    fun computeRevenueSnapshot() {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        try {
            revenueMetricsService.computeAndSaveDailySnapshot(yesterday)
            log.debug("Revenue snapshot computed for {}", yesterday)
        } catch (e: Exception) {
            log.error("Revenue snapshot job failed for {}", yesterday, e)
        }
    }

    /** Run daily at 05:00 UTC; reconcile DB subscription state with Stripe/Razorpay. */
    @Scheduled(cron = "\${app.subscription.reconciliation-cron:0 0 5 * * ?}")
    fun runSubscriptionReconciliation() {
        try {
            val mismatches = subscriptionReconciliationService.reconcileAll()
            if (mismatches > 0) log.warn("Subscription reconciliation found {} mismatch(es)", mismatches)
        } catch (e: Exception) {
            log.error("Subscription reconciliation job failed", e)
        }
    }
}
