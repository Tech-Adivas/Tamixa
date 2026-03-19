package com.tamixa.api.subscription.dto

import jakarta.validation.constraints.Size

data class UpgradeRequest(
    @field:Size(max = 2048, message = "successUrl must not exceed 2048 characters")
    val successUrl: String? = null,
    @field:Size(max = 2048, message = "cancelUrl must not exceed 2048 characters")
    val cancelUrl: String? = null,
    @field:Size(max = 32, message = "referralCode must not exceed 32 characters")
    val referralCode: String? = null
)
