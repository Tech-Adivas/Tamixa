package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

/** Optional body for request-changes and review-reject. */
data class ReviewNotesRequest(
    @field:Size(max = 2000)
    val notes: String? = null
)
