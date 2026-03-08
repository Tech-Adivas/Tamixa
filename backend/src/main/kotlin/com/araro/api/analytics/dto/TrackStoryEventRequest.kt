package com.araro.api.analytics.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class TrackStoryEventRequest(
    @field:NotNull
    val storyId: Long,

    @field:NotBlank
    @field:Pattern(regexp = "curated|generated", message = "storySource must be 'curated' or 'generated'")
    val storySource: String = "generated",

    /** Optional: for per-child achievements. Parent selects which child is listening. */
    val childId: Long? = null,

    @field:NotBlank
    val language: String,

    @field:NotBlank
    @field:Pattern(
        regexp = "story_started|story_25_percent|story_50_percent|story_75_percent|story_completed|story_stopped_early",
        message = "Invalid event_type"
    )
    val eventType: String,

    @field:Min(0)
    @field:Max(86400)  // max 24h in seconds
    val playbackPositionSeconds: Int = 0
)
