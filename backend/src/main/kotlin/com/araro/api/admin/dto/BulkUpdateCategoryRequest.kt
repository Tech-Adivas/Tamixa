package com.araro.api.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class BulkUpdateCategoryRequest(
    @field:NotEmpty(message = "At least one story ID is required")
    @field:Size(max = 100)
    val ids: List<Long>,

    @field:NotBlank(message = "Category/theme is required")
    @field:Size(max = 100)
    val theme: String
)
