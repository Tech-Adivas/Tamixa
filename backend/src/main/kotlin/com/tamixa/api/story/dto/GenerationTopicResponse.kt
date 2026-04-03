package com.tamixa.api.story.dto

/** Parent-facing catalog entry for safe server-defined story topics. */
data class GenerationTopicResponse(
    val id: String,
    val theme: String,
    val suggestedLearningFocus: String? = null,
    val descriptionEn: String? = null,
)
