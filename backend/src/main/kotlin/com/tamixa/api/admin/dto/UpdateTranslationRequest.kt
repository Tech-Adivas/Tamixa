package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

/**
 * Request to update a story translation's content (per-language edit in Story for review).
 */
data class UpdateTranslationRequest(
    @field:Size(max = 255)
    val title: String? = null,
    val content: String? = null,
    @field:Size(max = 2000)
    val moral: String? = null
)
