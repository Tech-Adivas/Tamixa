package com.tamixa.api.library.dto

import jakarta.validation.constraints.Size

/**
 * Request body for story workflow operations that accept optional notes.
 * Used for: submit, approve, request-changes, reject operations.
 */
data class StoryWorkflowRequest(
    @field:Size(max = 2000, message = "Notes must not exceed 2000 characters")
    val notes: String? = null
)
