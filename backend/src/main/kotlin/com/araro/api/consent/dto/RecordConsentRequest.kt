package com.araro.api.consent.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class RecordConsentRequest(
    @field:NotBlank(message = "consentType is required")
    val consentType: String,
    @field:PositiveOrZero
    val version: Int? = 1
)
