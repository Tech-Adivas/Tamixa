package com.tamixa.api.story.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for POST /api/v1/stories/{id}/remix.
 * Mobile sends remixInstruction (e.g. "make the dragon friendly").
 */
data class RemixRequest(
    @field:NotBlank(message = "Remix instruction is required")
    @field:Size(max = 500, message = "Remix instruction must not exceed 500 characters")
    val remixInstruction: String
)
