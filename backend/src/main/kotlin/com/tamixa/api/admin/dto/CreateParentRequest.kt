package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateParentRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    val password: String,

    @field:Size(max = 20)
    val phone: String? = null,

    @field:Pattern(
        regexp = "PARENT|ADMIN|SUPER_ADMIN|REVENUE_ANALYST|CONTENT_MANAGER|SUPPORT",
        message = "Role must be one of: PARENT, ADMIN, SUPER_ADMIN, REVENUE_ANALYST, CONTENT_MANAGER, SUPPORT"
    )
    val role: String? = "PARENT"
)
