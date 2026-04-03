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
    /** Rewritten narration script (conversational) when pipeline has run. */
    val narratedContent: String? = null,
    /**
     * Primary story text for this language (translation row or master), excluding the narration script.
     * Admin editors show this in “Story text” and [narratedContent] in “Narration script”.
     */
    val sourceContent: String = "",
    /**
     * Hint that a conversational narration script exists (post–rewrite pipeline).
     */
    val preferNarratedContentForEditor: Boolean = false,
    /** When set, admin has marked this story for reject from the language view. */
    val rejectMarkedAt: Instant? = null,
    /** When true, Regenerate with prompt is gated for content managers until approved by elevated admin. */
    val regeneratePromptLocked: Boolean = false,
    val regeneratePromptLockApproved: Boolean = false,
    val regeneratePromptUnlockRequestedAt: Instant? = null,
    /** When set, story is in trash (soft-deleted) until retention expires or restored. */
    val deletedAt: Instant? = null,
    /** Dinner-table style prompts for parents (curated library). */
    val parentDiscussionPrompts: List<String>? = null,
    /** Note on simplification or dramatization for listeners. */
    val parentContentNote: String? = null,
    /** Short invitation to try speaking aloud after the story. */
    val speakAlongPrompt: String? = null,
)
