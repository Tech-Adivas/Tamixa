package com.araro.infrastructure.stripe

import com.araro.application.port.ParentRepositoryPort
import com.araro.application.port.SubscriptionCheckoutPort
import com.araro.application.port.SubscriptionRepositoryPort
import com.araro.infrastructure.config.AppProperties
import com.stripe.Stripe
import com.stripe.param.checkout.SessionCreateParams
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Creates Stripe Checkout Session for subscription upgrade.
 * Requires app.subscription.stripe.enabled=true and secretKey.
 */
@Component
@ConditionalOnProperty(name = ["app.subscription.stripe.enabled"], havingValue = "true")
class StripeCheckoutAdapter(
    private val appProperties: AppProperties,
    private val parentRepository: ParentRepositoryPort,
    private val subscriptionRepository: SubscriptionRepositoryPort
) : SubscriptionCheckoutPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun createCheckoutSession(parentId: Long, successUrl: String, cancelUrl: String): String? {
        val stripe = appProperties.subscription.stripe
        if (stripe.secretKey.isBlank()) {
            log.warn("Stripe secret key not configured")
            return null
        }
        Stripe.apiKey = stripe.secretKey

        val parent = parentRepository.findById(parentId) ?: return null
        val sub = subscriptionRepository.findByParentId(parentId)
        val customerId = sub?.externalCustomerId

        val priceId = stripe.voicePremiumPriceId.takeIf { it.isNotBlank() }
            ?: return null.also { log.warn("Stripe voicePremiumPriceId not configured") }

        val lineItem = SessionCreateParams.LineItem.builder()
            .setPrice(priceId)
            .setQuantity(1L)
            .build()

        val paramsBuilder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(lineItem)
            .putMetadata("parent_id", parentId.toString())

        if (customerId != null) {
            paramsBuilder.setCustomer(customerId)
        } else {
            paramsBuilder.setCustomerEmail(parent.email)
        }

        return try {
            val params = paramsBuilder.build()
            val session = com.stripe.model.checkout.Session.create(params, null)
            session.url
        } catch (e: Exception) {
            log.error("Failed to create Stripe checkout session: {}", e.message)
            null
        }
    }
}
