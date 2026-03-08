package com.araro.application.subscription

import com.araro.application.port.AuditLogPort
import com.araro.application.port.BillingAuditPort
import com.araro.application.port.InvoicePort
import com.araro.application.port.SubscriptionCheckoutPort
import com.araro.application.port.SubscriptionEventPort
import com.araro.application.port.SubscriptionRepositoryPort
import com.araro.domain.BillingEventType
import com.araro.domain.Invoice
import com.araro.domain.InvoiceStatus
import com.araro.domain.PaymentProvider
import com.araro.domain.Subscription
import com.araro.domain.SubscriptionPlan
import com.araro.domain.SubscriptionStatus
import com.araro.domain.SubscriptionEvent
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepositoryPort,
    private val subscriptionEventPort: SubscriptionEventPort,
    private val billingAuditPort: BillingAuditPort,
    private val invoicePort: InvoicePort,
    private val auditLog: AuditLogPort,
    private val appProperties: AppProperties,
    private val subscriptionCheckoutPort: SubscriptionCheckoutPort
) {

    fun createCheckoutSession(parentId: Long, successUrl: String, cancelUrl: String): String? =
        subscriptionCheckoutPort.createCheckoutSession(parentId, successUrl, cancelUrl)

    private val log = LoggerFactory.getLogger(javaClass)
    private val subscriptionConfig get() = appProperties.subscription

    /**
     * Returns the current subscription for the parent. Creates a FREE subscription if none exists.
     * Never trust client-side subscription state; always resolve server-side.
     */
    @Transactional
    fun getOrCreateSubscription(parentId: Long): Subscription {
        return subscriptionRepository.findByParentId(parentId)
            ?: createFreeSubscription(parentId)
    }

    @Transactional
    fun createFreeSubscription(parentId: Long): Subscription {
        val now = Instant.now()
        val sub = Subscription(
            id = 0,
            parentId = parentId,
            plan = SubscriptionPlan.FREE,
            status = SubscriptionStatus.FREE,
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = null,
            externalCustomerId = null,
            currentPeriodStart = now,
            currentPeriodEnd = null,
            trialEnd = null,
            cancelAtPeriodEnd = false,
            canceledAt = null,
            pastDueAt = null,
            maxChildren = 1,
            voicePremium = false,
            createdAt = now,
            updatedAt = now
        )
        val saved = subscriptionRepository.save(sub)
        appendEvent(saved, "subscription_created", null, SubscriptionStatus.FREE, null, null)
        auditLog.logSubscriptionChange(parentId, "created_free", "plan=FREE", null)
        return saved
    }

    /**
     * Transition subscription to a new state. Enforces state machine.
     */
    @Transactional
    fun transitionState(
        subscriptionId: Long,
        newStatus: SubscriptionStatus,
        externalEventId: String?,
        payload: String?,
        provider: PaymentProvider
    ): Subscription {
        val current = subscriptionRepository.findById(subscriptionId)
            ?: throw SubscriptionNotFoundException(subscriptionId)
        if (!SubscriptionStateMachine.canTransition(current.status, newStatus)) {
            throw InvalidSubscriptionTransitionException(current.status, newStatus)
        }
        val previousStatus = current.status
        val now = Instant.now()
        var pastDueAt = current.pastDueAt
        if (newStatus == SubscriptionStatus.PAST_DUE && current.pastDueAt == null) {
            pastDueAt = now
        }
        val updated = current.copy(
            status = newStatus,
            pastDueAt = pastDueAt,
            updatedAt = now
        )
        val saved = subscriptionRepository.save(updated)
        appendEvent(saved, "status_transition", previousStatus, newStatus, externalEventId, payload, payload, provider)
        auditLog.logSubscriptionChange(current.parentId, "status_transition", "$previousStatus -> $newStatus", null)
        return saved
    }

    /**
     * Mark subscription as canceled at period end (user-initiated).
     */
    @Transactional
    fun cancelAtPeriodEnd(subscriptionId: Long, parentId: Long): Subscription {
        val current = subscriptionRepository.findById(subscriptionId)
            ?: throw SubscriptionNotFoundException(subscriptionId)
        if (current.parentId != parentId) throw SubscriptionAccessDeniedException()
        if (current.status !in listOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL)) {
            throw InvalidSubscriptionTransitionException(current.status, SubscriptionStatus.CANCELED)
        }
        val now = Instant.now()
        val updated = current.copy(
            cancelAtPeriodEnd = true,
            canceledAt = now,
            updatedAt = now
        )
        val saved = subscriptionRepository.save(updated)
        appendEvent(saved, "cancel_at_period_end", current.status, null, null, null, null, current.provider)
        auditLog.logSubscriptionChange(parentId, "cancel_at_period_end", "subscriptionId=$subscriptionId", null)
        return saved
    }

    /**
     * Apply prorated upgrade: update plan/period and record billing event.
     */
    @Transactional
    fun applyProratedUpgrade(
        subscriptionId: Long,
        newPlan: SubscriptionPlan,
        newPeriodEnd: Instant,
        amountMinor: Long,
        currency: String,
        invoiceId: String?,
        provider: PaymentProvider
    ): Subscription {
        val current = subscriptionRepository.findById(subscriptionId)
            ?: throw SubscriptionNotFoundException(subscriptionId)
        val now = Instant.now()
        val updated = current.copy(
            plan = newPlan,
            currentPeriodEnd = newPeriodEnd,
            updatedAt = now
        )
        val saved = subscriptionRepository.save(updated)
        billingAuditPort.append(
            com.araro.domain.BillingAuditLog(
                id = 0,
                parentId = current.parentId,
                subscriptionId = current.id,
                eventType = BillingEventType.PRORATION_APPLIED,
                provider = provider,
                externalId = null,
                amountMinor = amountMinor,
                currency = currency,
                invoiceId = invoiceId,
                details = "upgrade_to_$newPlan",
                createdAt = now
            )
        )
        appendEvent(saved, "proration_applied", current.status, null, null, "plan=$newPlan", "plan=$newPlan", provider)
        return saved
    }

    /**
     * Grace period enforcement: PAST_DUE -> GRACE_PERIOD when entering grace window;
     * after grace expires (past_due_at + gracePeriodDays), transition to EXPIRED.
     * Financial safety: all transitions validated by state machine; run via scheduled job daily.
     */
    @Transactional
    fun enforceGracePeriodExpiry(subscriptionId: Long): Subscription? {
        val sub = subscriptionRepository.findById(subscriptionId) ?: return null
        if (sub.status != SubscriptionStatus.PAST_DUE && sub.status != SubscriptionStatus.GRACE_PERIOD) return sub
        val pastDueAt = sub.pastDueAt ?: return sub
        val graceEnd = pastDueAt.plusSeconds(subscriptionConfig.gracePeriodDays * 86400L)
        val now = Instant.now()
        if (now.isBefore(graceEnd)) {
            if (sub.status == SubscriptionStatus.PAST_DUE && SubscriptionStateMachine.canTransition(sub.status, SubscriptionStatus.GRACE_PERIOD)) {
                return transitionState(subscriptionId, SubscriptionStatus.GRACE_PERIOD, null, "entered_grace_period", sub.provider).let { subscriptionRepository.findById(subscriptionId) }
            }
            return sub
        }
        return transitionState(subscriptionId, SubscriptionStatus.EXPIRED, null, "grace_period_expired", sub.provider).let { subscriptionRepository.findById(subscriptionId) }
    }

    /**
     * Find subscription by provider and external id (for webhooks).
     */
    fun findByProviderAndExternalId(provider: PaymentProvider, externalSubscriptionId: String): Subscription? {
        return subscriptionRepository.findByProviderAndExternalId(provider.name, externalSubscriptionId)
    }

    /**
     * Scheduled job: enforce grace period expiry for all PAST_DUE/GRACE_PERIOD subscriptions.
     * After gracePeriodDays from past_due_at, transitions to EXPIRED. Run daily.
     */
    @Transactional
    fun enforceGracePeriodExpiryForAll(): Int {
        val ids = subscriptionRepository.findSubscriptionIdsByStatusIn(
            listOf(SubscriptionStatus.PAST_DUE, SubscriptionStatus.GRACE_PERIOD)
        )
        var expired = 0
        for (id in ids) {
            val before = subscriptionRepository.findById(id)?.status
            enforceGracePeriodExpiry(id)
            val after = subscriptionRepository.findById(id)?.status
            if (after == SubscriptionStatus.EXPIRED && before != SubscriptionStatus.EXPIRED) expired++
        }
        return expired
    }

    /**
     * Scheduled job: transition TRIAL subscriptions to EXPIRED when trial has ended. Run daily.
     */
    @Transactional
    fun expireEndedTrials(): Int {
        val now = Instant.now()
        val ids = subscriptionRepository.findSubscriptionIdsByStatusIn(listOf(SubscriptionStatus.TRIAL))
        var expired = 0
        for (id in ids) {
            val sub = subscriptionRepository.findById(id) ?: continue
            val trialEnd = sub.trialEnd ?: continue
            if (now.isAfter(trialEnd) && SubscriptionStateMachine.canTransition(sub.status, SubscriptionStatus.EXPIRED)) {
                transitionState(id, SubscriptionStatus.EXPIRED, null, "trial_ended", sub.provider)
                expired++
            }
        }
        return expired
    }

    /**
     * Create or update subscription from payment provider (webhook handler).
     * State machine: all status transitions from provider are validated; invalid transitions are rejected.
     */
    @Transactional
    fun upsertFromProvider(
        parentId: Long,
        plan: SubscriptionPlan,
        status: SubscriptionStatus,
        provider: PaymentProvider,
        externalSubscriptionId: String,
        externalCustomerId: String?,
        currentPeriodStart: Instant?,
        currentPeriodEnd: Instant?,
        trialEnd: Instant?,
        cancelAtPeriodEnd: Boolean,
        maxChildren: Int,
        voicePremium: Boolean
    ): Subscription {
        val existing = subscriptionRepository.findByProviderAndExternalId(provider.name, externalSubscriptionId)
        val now = Instant.now()
        return if (existing != null) {
            val statusToApply = if (SubscriptionStateMachine.canTransition(existing.status, status)) {
                status
            } else {
                log.warn("Invalid subscription transition from provider, keeping current status: {} -> {} (subscriptionId={} provider={})",
                    existing.status, status, existing.id, provider.name)
                existing.status
            }
            val trialUsed = existing.trialUsed || (statusToApply == SubscriptionStatus.TRIAL)
            val updated = existing.copy(
                plan = plan,
                status = statusToApply,
                externalCustomerId = externalCustomerId ?: existing.externalCustomerId,
                currentPeriodStart = currentPeriodStart ?: existing.currentPeriodStart,
                currentPeriodEnd = currentPeriodEnd ?: existing.currentPeriodEnd,
                trialEnd = trialEnd ?: existing.trialEnd,
                cancelAtPeriodEnd = cancelAtPeriodEnd,
                maxChildren = maxChildren,
                voicePremium = voicePremium,
                trialUsed = trialUsed,
                updatedAt = now
            )
            subscriptionRepository.save(updated)
        } else {
            val trialUsed = status == SubscriptionStatus.TRIAL
            val newSub = Subscription(
                id = 0,
                parentId = parentId,
                plan = plan,
                status = status,
                provider = provider,
                externalSubscriptionId = externalSubscriptionId,
                externalCustomerId = externalCustomerId,
                currentPeriodStart = currentPeriodStart,
                currentPeriodEnd = currentPeriodEnd,
                trialEnd = trialEnd,
                cancelAtPeriodEnd = cancelAtPeriodEnd,
                canceledAt = null,
                pastDueAt = null,
                maxChildren = maxChildren,
                voicePremium = voicePremium,
                trialUsed = trialUsed,
                createdAt = now,
                updatedAt = now
            )
            subscriptionRepository.save(newSub)
        }
    }

    /**
     * Record payment succeeded for revenue and audit; persist invoice for financial audit.
     */
    @Transactional
    fun recordPaymentSucceeded(
        parentId: Long,
        subscriptionId: Long?,
        provider: PaymentProvider,
        externalId: String?,
        amountMinor: Long,
        currency: String?,
        invoiceId: String?
    ) {
        billingAuditPort.append(
            com.araro.domain.BillingAuditLog(
                id = 0,
                parentId = parentId,
                subscriptionId = subscriptionId,
                eventType = BillingEventType.PAYMENT_SUCCEEDED,
                provider = provider,
                externalId = externalId,
                amountMinor = amountMinor,
                currency = currency,
                invoiceId = invoiceId,
                details = null,
                createdAt = Instant.now()
            )
        )
        if (invoiceId != null && currency != null) {
            invoicePort.save(
                Invoice(
                    id = 0,
                    providerInvoiceId = invoiceId,
                    parentId = parentId,
                    subscriptionId = subscriptionId,
                    amountMinor = amountMinor,
                    currency = currency,
                    status = InvoiceStatus.PAID,
                    paidAt = Instant.now(),
                    createdAt = Instant.now()
                )
            )
        }
    }

    /**
     * Record refund for financial audit. Subscription state unchanged.
     * Edge case: manual refund in Stripe dashboard; we log for audit.
     */
    @Transactional
    fun recordRefund(
        parentId: Long,
        subscriptionId: Long?,
        provider: PaymentProvider,
        externalId: String?,
        amountMinor: Long,
        invoiceId: String?
    ) {
        billingAuditPort.append(
            com.araro.domain.BillingAuditLog(
                id = 0,
                parentId = parentId,
                subscriptionId = subscriptionId,
                eventType = BillingEventType.PAYMENT_REFUNDED,
                provider = provider,
                externalId = externalId,
                amountMinor = amountMinor,
                currency = null,
                invoiceId = invoiceId,
                details = "refund",
                createdAt = Instant.now()
            )
        )
    }

    /**
     * Record payment failed and optionally move subscription to PAST_DUE.
     */
    @Transactional
    fun recordPaymentFailed(
        parentId: Long,
        subscriptionId: Long?,
        provider: PaymentProvider,
        externalId: String?,
        details: String?
    ) {
        billingAuditPort.append(
            com.araro.domain.BillingAuditLog(
                id = 0,
                parentId = parentId,
                subscriptionId = subscriptionId,
                eventType = BillingEventType.PAYMENT_FAILED,
                provider = provider,
                externalId = externalId,
                amountMinor = null,
                currency = null,
                invoiceId = null,
                details = details,
                createdAt = Instant.now()
            )
        )
        if (subscriptionId != null) {
            val sub = subscriptionRepository.findById(subscriptionId) ?: return
            if (SubscriptionStateMachine.canTransition(sub.status, SubscriptionStatus.PAST_DUE)) {
                transitionState(subscriptionId, SubscriptionStatus.PAST_DUE, externalId, details, provider)
            }
        }
    }

    /** Appends to subscription_events for financial audit; reason documents why the transition occurred. */
    private fun appendEvent(
        sub: Subscription,
        eventType: String,
        previousStatus: SubscriptionStatus?,
        newStatus: SubscriptionStatus?,
        externalEventId: String?,
        payload: String? = null,
        reason: String? = null,
        provider: PaymentProvider = sub.provider
    ) {
        subscriptionEventPort.append(
            SubscriptionEvent(
                id = 0,
                subscriptionId = sub.id,
                eventType = eventType,
                previousStatus = previousStatus,
                newStatus = newStatus,
                reason = reason ?: payload,
                provider = provider,
                externalEventId = externalEventId,
                payload = payload,
                createdAt = Instant.now()
            )
        )
    }
}

class SubscriptionNotFoundException(id: Long) : RuntimeException("Subscription not found: $id")
class SubscriptionAccessDeniedException : RuntimeException("Access denied to this subscription")
class InvalidSubscriptionTransitionException(from: SubscriptionStatus, to: SubscriptionStatus) :
    RuntimeException("Invalid transition: $from -> $to")
