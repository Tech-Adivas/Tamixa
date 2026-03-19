package com.tamixa.application.storylibrary

/**
 * Canonical story lifecycle status constants.
 * Use these instead of magic strings to avoid typos and ensure consistency.
 */
object StoryStatus {
    const val DRAFT = "DRAFT"
    const val PUBLISHED = "PUBLISHED"
    const val PROCESSING = "PROCESSING"
    const val READY = "READY"
    const val CHANGES_REQUESTED = "CHANGES_REQUESTED"
    const val REJECTED = "REJECTED"

    /** Statuses that indicate story is in the review queue (awaiting approve/reject). */
    val REVIEW_QUEUE = setOf(PUBLISHED, PROCESSING, READY)

    /** Statuses that mean admin already acted; pipeline must not overwrite them. */
    val TERMINAL_REVIEW = setOf(CHANGES_REQUESTED, REJECTED)

    /** Statuses for which "content unchanged" check applies when resubmitting for review. */
    val CONTENT_SAME_CHECK = setOf(PUBLISHED, READY, CHANGES_REQUESTED)

    /** Statuses allowed from client request (DRAFT or PUBLISHED). */
    val ALLOWED_FROM_REQUEST = setOf(DRAFT, PUBLISHED)
}
