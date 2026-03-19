package com.tamixa.api.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordlessVerifyRequest(
    @field:NotBlank(message = "Email is required")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "Code is required")
    @field:Size(max = 20, message = "Code must not exceed 20 characters")
    val code: String,

    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val acceptedParentalAttestation: Boolean = false
)
