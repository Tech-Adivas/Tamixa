package com.araro.api.stream.dto

/**
 * Stream URL and optional word-level timings for transcript sync.
 * When [wordTimings] is present, the app can highlight words in sync with the voice.
 */
data class StreamUrlResponse(
    val streamUrl: String,
    val avatarUrl: String? = null,
    val avatarVideoUrl: String? = null,
    val wordTimings: List<WordTimingDto>? = null
)
