package com.araro.api.admin.dto

import jakarta.validation.constraints.Size

/**
 * Request body for suggesting rephrased title and content.
 * Both fields are optional; when omitted, existing story values are used.
 */
data class SuggestRephraseRequest(
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String? = null,
    @field:Size(max = 50_000, message = "Content must not exceed 50000 characters")
    val content: String? = null
)
