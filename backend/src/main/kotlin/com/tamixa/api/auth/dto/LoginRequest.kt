package com.tamixa.api.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(max = 500, message = "Password must not exceed 500 characters")
    val password: String,

    /** Optional device fingerprint (hashed) for fraud prevention. Stored on login. */
    val deviceFingerprint: String? = null
)
