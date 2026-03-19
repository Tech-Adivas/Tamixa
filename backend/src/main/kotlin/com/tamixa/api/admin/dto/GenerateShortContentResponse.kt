package com.tamixa.api.admin.dto

data class GeneratedShortContentItem(
    val content: String,
    val answer: String? = null
)

data class GenerateShortContentResponse(
    val items: List<GeneratedShortContentItem>,
    /** Number of generated items rejected by content moderation. */
    val rejectedCount: Int = 0
)
