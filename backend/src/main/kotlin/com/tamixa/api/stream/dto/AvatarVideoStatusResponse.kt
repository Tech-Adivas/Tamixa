package com.tamixa.api.stream.dto

/**
 * Talking video job status for mobile polling.
 * Per personalization flow: User Uploads Avatar → Talking Video Job → Generated Video.
 */
data class AvatarVideoStatusResponse(
    val status: String,
    val videoUrl: String?,
    val errorMessage: String?
)
