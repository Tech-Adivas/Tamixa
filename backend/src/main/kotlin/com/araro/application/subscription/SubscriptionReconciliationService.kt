package com.araro.application.subscription

import com.araro.application.port.SubscriptionReconciliationPort
import com.araro.application.port.SubscriptionRepositoryPort
import com.araro.domain.PaymentProvider
import com.araro.domain.Subscription
import com.araro.domain.SubscriptionReconciliationLog
import com.araro.domain.SubscriptionStatus
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.persistence.SubscriptionJpaRepository
import com.stripe.Stripe
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Reconciliation: compare DB subscription state with payment provider (Stripe/Razorpay).
 * Financial safety: mismatches logged for audit; handles webhook delays and manual dashboard changes.
 */
@Service
class SubscriptionReconciliationService(
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val reconciliationPort: SubscriptionReconciliationPort,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Reconcile all subscriptions with external IDs; return count of mismatches. */
    @Transactional
    fun reconcileAll(): Int {
        val subs = subscriptionJpaRepository.findByExternalSubscriptionIdIsNotNull()
        var mismatches = 0
        for (entity in subs) {
            val sub = entity.toDomain()
            val providerStatus = fetchProviderStatus(sub)
            if (providerStatus != null) {
                val dbStatus = sub.status.name
                val mappedStatus = mapProviderStatus(providerStatus, sub.provider)
                val mismatch = mappedStatus != sub.status
                reconciliationPort.append(
                    SubscriptionReconciliationLog(
                        id = 0,
                        subscriptionId = sub.id,
                        providerStatus = providerStatus,
                        dbStatus = dbStatus,
                        mismatchFlag = mismatch,
                        checkedAt = Instant.now()
                    )
                )
                if (mismatch) {
                    mismatches++
                    log.warn(
                        "Subscription reconciliation mismatch: subId={} provider={} providerStatus={} dbStatus={}",
                        sub.id, sub.provider, providerStatus, dbStatus
                    )
                }
            }
        }
        return mismatches
    }

    private fun fetchProviderStatus(sub: com.araro.domain.Subscription): String? {
        val externalId = sub.externalSubscriptionId ?: return null
        return when (sub.provider) {
            PaymentProvider.STRIPE -> fetchStripeStatus(externalId)
            PaymentProvider.ZOHO -> fetchZohoStatus(externalId)
        }
    }

    private fun fetchStripeStatus(subscriptionId: String): String? {
        val secretKey = appProperties.subscription.stripe.secretKey
        if (!appProperties.subscription.stripe.enabled || secretKey.isBlank()) return null
        return try {
            Stripe.apiKey = secretKey
            val stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId)
            stripeSub.status
        } catch (e: Exception) {
            log.debug("Stripe subscription fetch failed for {}: {}", subscriptionId, e.message)
            null
        }
    }

    private fun fetchZohoStatus(paymentId: String): String? {
        if (!appProperties.subscription.zoho.enabled || appProperties.subscription.zoho.accountId.isBlank()) {
            return null
        }
        return null
    }

    private fun mapProviderStatus(providerStatus: String, provider: PaymentProvider): SubscriptionStatus =
        when (provider) {
            PaymentProvider.STRIPE -> when (providerStatus.lowercase()) {
                "trialing" -> SubscriptionStatus.TRIAL
                "active" -> SubscriptionStatus.ACTIVE
                "past_due" -> SubscriptionStatus.PAST_DUE
                "canceled", "unpaid", "incomplete_expired" -> SubscriptionStatus.EXPIRED
                else -> SubscriptionStatus.ACTIVE
            }
            PaymentProvider.ZOHO -> when (providerStatus.lowercase()) {
                "succeeded", "captured" -> SubscriptionStatus.ACTIVE
                "failed", "refunded" -> SubscriptionStatus.EXPIRED
                else -> SubscriptionStatus.ACTIVE
            }
        }

    private fun com.araro.infrastructure.persistence.SubscriptionEntity.toDomain(): com.araro.domain.Subscription =
        com.araro.domain.Subscription(
            id = id,
            parentId = parent.id,
            plan = plan,
            status = status,
            provider = provider,
            externalSubscriptionId = externalSubscriptionId,
            externalCustomerId = externalCustomerId,
            currentPeriodStart = currentPeriodStart,
            currentPeriodEnd = currentPeriodEnd,
            trialEnd = trialEnd,
            cancelAtPeriodEnd = cancelAtPeriodEnd,
            canceledAt = canceledAt,
            pastDueAt = pastDueAt,
            maxChildren = maxChildren,
            voicePremium = voicePremium,
            trialUsed = trialUsed,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
