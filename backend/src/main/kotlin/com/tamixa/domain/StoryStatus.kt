package com.tamixa.domain

/**
 * Story lifecycle: generation pipeline then async audio.
 * Generation: REQUESTED → GENERATING → MODERATION_CHECK → PENDING | PENDING_REVIEW | FAILED
 * When [app.story.human-review-before-narration] is true: content lands in PENDING_REVIEW until admin approves,
 * then PENDING and async narration runs ([StoryEventPublisherPort.publishStoryCreated]).
 * Audio: PENDING → PROCESSING → READY | FAILED
 * Moderation: Admin can FLAG (from READY) or APPROVE→READY / REJECT→FAILED; PENDING_REVIEW + approve → PENDING + narration.
 */
enum class StoryStatus {
    REQUESTED,
    GENERATING,
    MODERATION_CHECK,
    PENDING,
    /** Generated text passed automated checks; awaiting human approval before narration pipeline. */
    PENDING_REVIEW,
    PROCESSING,
    READY,
    FAILED,
    FLAGGED
}
