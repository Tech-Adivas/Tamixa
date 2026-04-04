package com.tamixa.api.analytics.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Lightweight app analytics. No PII. */
data class TrackAppEventRequest(
    @field:NotBlank
    @field:Pattern(
        regexp = "screen_view|search|favorite_add|favorite_remove|library_hub",
        message = "eventType must be a supported app event"
    )
    val eventType: String,

    @field:Size(max = 64)
    val screenName: String? = null,

    /** Query length only – never the raw query to avoid PII. */
    val searchQueryLength: Int? = null,

    val storyId: Long? = null,

    @field:Pattern(regexp = "curated|generated", message = "storySource must be curated or generated")
    val storySource: String? = null,

    /** Required when [eventType] is `library_hub` (Browse / Fun / Learn / Practice lanes). Must be absent for other types. */
    @field:Size(max = 32)
    @field:Pattern(
        regexp = "browse|fun|learn|simulator|learn_safety",
        message = "hubKey must be browse, fun, learn, simulator, or learn_safety"
    )
    val hubKey: String? = null,
)
