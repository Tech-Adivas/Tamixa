package com.araro.api.subscription.dto

import jakarta.validation.constraints.Size

data class UpgradeRequest(
    @field:Size(max = 2048, message = "successUrl must not exceed 2048 characters")
    val successUrl: String? = null,
    @field:Size(max = 2048, message = "cancelUrl must not exceed 2048 characters")
    val cancelUrl: String? = null
)
