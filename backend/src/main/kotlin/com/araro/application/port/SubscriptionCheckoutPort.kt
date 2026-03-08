package com.araro.application.port

/**
 * Creates payment checkout session for subscription upgrade.
 * Returns Stripe Checkout URL or null when not configured.
 */
interface SubscriptionCheckoutPort {
    fun createCheckoutSession(parentId: Long, successUrl: String, cancelUrl: String): String?
}
