package com.tamixa.application.port

/**
 * Creates payment checkout session for subscription upgrade.
 * Returns Stripe Checkout URL or null when not configured.
 * @param stripeCouponId Optional Stripe coupon ID to apply discount (from validated referral code).
 */
interface SubscriptionCheckoutPort {
    fun createCheckoutSession(
        parentId: Long,
        successUrl: String,
        cancelUrl: String,
        stripeCouponId: String? = null
    ): String?
}
