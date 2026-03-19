package com.tamixa.api.admin.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class MarkReviewedLanguagesRequest(
    @field:NotEmpty(message = "At least one language is required")
    @field:Size(max = 20)
    val languages: List<String>
)
