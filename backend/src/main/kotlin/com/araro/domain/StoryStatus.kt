package com.araro.domain

/**
 * Story lifecycle: generation pipeline then async audio.
 * Generation: REQUESTED → GENERATING → MODERATION_CHECK → PENDING | FAILED
 * Audio: PENDING → PROCESSING → READY | FAILED
 * Moderation: Admin can FLAG (from READY) or APPROVE→READY / REJECT→FAILED.
 */
enum class StoryStatus {
    REQUESTED,
    GENERATING,
    MODERATION_CHECK,
    PENDING,
    PROCESSING,
    READY,
    FAILED,
    FLAGGED
}
