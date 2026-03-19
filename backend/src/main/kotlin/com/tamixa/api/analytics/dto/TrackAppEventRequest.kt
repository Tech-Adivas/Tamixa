package com.tamixa.api.analytics.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Lightweight app analytics. No PII. */
data class TrackAppEventRequest(
    @field:NotBlank
    @field:Pattern(
        regexp = "screen_view|search|favorite_add|favorite_remove",
        message = "eventType must be screen_view, search, favorite_add, or favorite_remove"
    )
    val eventType: String,

    @field:Size(max = 64)
    val screenName: String? = null,

    /** Query length only – never the raw query to avoid PII. */
    val searchQueryLength: Int? = null,

    val storyId: Long? = null,

    @field:Pattern(regexp = "curated|generated", message = "storySource must be curated or generated")
    val storySource: String? = null
)
