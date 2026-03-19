package com.tamixa.domain

import java.time.Instant

/**
 * Referral/promo code for subscription discount: shop name, shortcode (e.g. AMAZ5, SHOPSTOP10), offer percentage, expiration.
 */
data class ReferralCode(
    val id: Long,
    val shortcode: String,
    val shopName: String,
    val offerPercent: Int,
    val expiresAt: Instant,
    val active: Boolean,
    val stripeCouponId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
