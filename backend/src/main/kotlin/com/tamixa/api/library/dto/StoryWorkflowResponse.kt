package com.tamixa.api.library.dto

/**
 * Response for story workflow operations.
 * Provides consistent structure for all workflow endpoints.
 */
data class StoryWorkflowResponse(
    val success: Boolean,
    val message: String,
    val status: String? = null,
    val storyId: Long? = null
)
