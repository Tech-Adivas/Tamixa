package com.araro.api.admin.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class AddAdminUserRequest(
    @field:NotNull(message = "Parent ID is required")
    val parentId: Long,
    @field:Pattern(
        regexp = "SUPER_ADMIN|REVENUE_ANALYST|CONTENT_MANAGER|SUPPORT",
        message = "Role must be one of: SUPER_ADMIN, REVENUE_ANALYST, CONTENT_MANAGER, SUPPORT"
    )
    val role: String
)
