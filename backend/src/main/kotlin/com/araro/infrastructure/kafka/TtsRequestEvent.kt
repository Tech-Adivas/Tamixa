package com.araro.infrastructure.kafka

/**
 * Request to generate TTS for translated story content.
 * Event key for exactly-once: "${masterStoryId}:${language}"
 */
data class TtsRequestEvent(
    val masterStoryId: Long,
    val language: String,
    val content: String
)
