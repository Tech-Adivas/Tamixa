package com.tamixa.api.admin.dto

import java.time.Instant

data class LibraryStoryResponse(
    val id: Long,
    val title: String?,
    val content: String,
    val theme: String,
    val category: String? = null,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val moral: String?,
    val audioFileUrl: String?,
    val status: String,
    val coverImageUrl: String?,
    val coverVideoUrl: String? = null,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val storyOwner: String? = null,
    val convertPromptUsed: String? = null,
    val emotionMode: String? = null,
    /** When set, narration was approved for final delivery by a human. */
    val narrationApprovedAt: Instant? = null,
    /** Reviewer feedback when status is CHANGES_REQUESTED or REJECTED. */
    val reviewNotes: String? = null,
    /** Per-language narration approval: language code -> approved. */
    val translationApproval: Map<String, Boolean> = emptyMap(),
    /** Rewritten narration script (conversational) when pipeline has run. Use for further edits in library story form. */
    val narratedContent: String? = null,
    /** When set, admin has marked this story for reject from the language view. */
    val rejectMarkedAt: Instant? = null
)
