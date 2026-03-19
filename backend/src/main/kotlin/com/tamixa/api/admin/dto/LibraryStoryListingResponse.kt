package com.tamixa.api.admin.dto

import java.time.Instant

/**
 * Lightweight story listing response (projection).
 * Excludes content and moral to avoid loading full text in list queries.
 */
data class LibraryStoryListingResponse(
    val id: Long,
    val title: String?,
    val theme: String,
    val category: String? = null,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val audioFileUrl: String?,
    val status: String,
    val coverImageUrl: String?,
    val coverVideoUrl: String? = null,
    val createdAt: Instant
)
