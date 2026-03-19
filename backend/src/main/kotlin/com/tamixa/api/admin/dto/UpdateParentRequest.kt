package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateParentRequest(
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255)
    val email: String? = null,

    @field:Size(max = 20)
    val phone: String? = null,

    @field:Pattern(
        regexp = "PARENT|ADMIN|SUPER_ADMIN|REVENUE_ANALYST|CONTENT_MANAGER|SUPPORT",
        message = "Role must be one of: PARENT, ADMIN, SUPER_ADMIN, REVENUE_ANALYST, CONTENT_MANAGER, SUPPORT"
    )
    val role: String? = null
)
