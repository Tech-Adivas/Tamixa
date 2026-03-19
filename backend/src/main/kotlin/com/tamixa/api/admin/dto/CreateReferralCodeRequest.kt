package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateReferralCodeRequest(
    @field:NotBlank(message = "shortcode is required")
    @field:Size(min = 1, max = 32)
    val shortcode: String,

    @field:NotBlank(message = "shopName is required")
    @field:Size(max = 255)
    val shopName: String,

    @field:Min(0, message = "offerPercent must be 0-100")
    @field:Max(100, message = "offerPercent must be 0-100")
    val offerPercent: Int,

    val expiresAt: Instant,

    val active: Boolean = true
)
