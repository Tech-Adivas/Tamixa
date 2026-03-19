package com.tamixa.api.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    @field:Size(max = 2048, message = "Refresh token must not exceed 2048 characters")
    val refreshToken: String
)
