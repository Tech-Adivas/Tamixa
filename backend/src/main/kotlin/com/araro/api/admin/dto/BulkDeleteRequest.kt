package com.araro.api.admin.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class BulkDeleteRequest(
    @field:NotEmpty(message = "At least one story ID is required")
    @field:Size(max = 100)
    val ids: List<Long>
)
