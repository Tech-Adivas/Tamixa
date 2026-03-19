package com.tamixa.api.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OtpSendRequest(
    @field:NotBlank(message = "Phone is required")
    @field:Size(max = 20, message = "Phone must not exceed 20 characters")
    val phone: String
)
