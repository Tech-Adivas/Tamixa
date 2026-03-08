package com.araro.api.auth.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,

    /** Optional device fingerprint (hashed) for fraud prevention. Stored on login. */
    val deviceFingerprint: String? = null
)
