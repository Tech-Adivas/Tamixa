package com.tamixa.infrastructure.persistence

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "referral_codes")
data class ReferralCodeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 32)
    val shortcode: String,

    @Column(nullable = false, length = 255)
    val shopName: String,

    @Column(nullable = false)
    val offerPercent: Int,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "stripe_coupon_id", length = 255)
    val stripeCouponId: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
)
