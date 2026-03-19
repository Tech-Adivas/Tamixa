package com.tamixa.domain

import java.time.Instant

data class LibraryStory(
    val id: Long,
    val title: String?,
    val content: String,
    val theme: String,
    /** Content category for browse/filter; distinct from theme. */
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
    val updatedAt: Instant,
    val storyOwner: String? = null,
    val convertPromptUsed: String? = null,
    /** CALM, SOOTHING, ADVENTUROUS. Influences narration ToneMode. */
    val emotionMode: String? = null,
    /** When set, story narration has been verified and approved for final delivery by a human. */
    val narrationApprovedAt: Instant? = null,
    /** Reviewer feedback when status is CHANGES_REQUESTED or REJECTED. */
    val reviewNotes: String? = null,
    /** When set, admin has marked this story for reject from the language view (enables Reject button). */
    val rejectMarkedAt: Instant? = null
)
