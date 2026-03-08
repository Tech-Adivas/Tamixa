package com.araro.application.story

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Structured JSON output from the AI story generation API.
 * Contract: title, moral, story_text, estimated_duration_seconds.
 * Validate before saving; reject if structure invalid.
 */
data class StructuredStoryPayload(
    val title: String,
    val moral: String,
    @JsonProperty("story_text") val storyText: String,
    @JsonProperty("estimated_duration_seconds") val estimatedDurationSeconds: Int
) {
    /** Duration in minutes for reading time display. */
    fun durationMinutes(): Double = estimatedDurationSeconds / 60.0
}
