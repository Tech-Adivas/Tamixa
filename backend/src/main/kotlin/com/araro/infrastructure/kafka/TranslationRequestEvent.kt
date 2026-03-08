package com.araro.infrastructure.kafka

/**
 * Request to translate a curated story to a target language.
 * Event key for exactly-once: "${masterStoryId}:${language}"
 */
data class TranslationRequestEvent(
    val masterStoryId: Long,
    val language: String,
    val sourceContent: String,
    val sourceTitle: String?,
    val sourceMoral: String?
)
