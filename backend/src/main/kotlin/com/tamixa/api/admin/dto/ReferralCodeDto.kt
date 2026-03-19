package com.tamixa.api.admin.dto

import java.time.Instant

data class ReferralCodeDto(
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
