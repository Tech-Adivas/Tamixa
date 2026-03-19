package com.tamixa.api.subscription.dto

data class ReferralCodeValidateResponse(
    val valid: Boolean,
    val shortcode: String? = null,
    val shopName: String? = null,
    val offerPercent: Int? = null
)
