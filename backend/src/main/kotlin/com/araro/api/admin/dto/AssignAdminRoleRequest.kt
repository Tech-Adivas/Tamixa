package com.araro.api.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AssignAdminRoleRequest(
    @field:NotBlank(message = "Role is required")
    @field:Pattern(
        regexp = "SUPER_ADMIN|REVENUE_ANALYST|CONTENT_MANAGER|SUPPORT|PARENT",
        message = "Role must be one of: SUPER_ADMIN, REVENUE_ANALYST, CONTENT_MANAGER, SUPPORT, PARENT"
    )
    val role: String
)
