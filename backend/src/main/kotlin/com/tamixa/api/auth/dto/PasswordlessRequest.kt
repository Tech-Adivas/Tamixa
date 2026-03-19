package com.tamixa.api.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordlessRequest(
    @field:NotBlank(message = "Email is required")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String
)
