package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant

data class UpdateReferralCodeRequest(
    @field:Size(min = 1, max = 32)
    val shortcode: String? = null,

    @field:Size(max = 255)
    val shopName: String? = null,

    @field:Min(0, message = "offerPercent must be 0-100")
    @field:Max(100, message = "offerPercent must be 0-100")
    val offerPercent: Int? = null,

    val expiresAt: Instant? = null,

    val active: Boolean? = null
)
