package com.tamixa.domain

/**
 * Unified library story workflow status system.
 * Replaces the confusing separation between story status and pipeline status.
 * 
 * Workflow progression:
 * DRAFT → SUBMITTED → TRANSLATING → CONTENT_REVIEW → APPROVED → AUDIO_GENERATING → AUDIO_REVIEW → PUBLISHED
 * 
 * Error states: TRANSLATION_FAILED, AUDIO_FAILED
 * Review rejection: CHANGES_REQUESTED, REJECTED
 * 
 * Phase grouping:
 * - Draft: DRAFT
 * - Translation: SUBMITTED, TRANSLATING, TRANSLATION_FAILED
 * - Content Review: CONTENT_REVIEW, CHANGES_REQUESTED, REJECTED
 * - Audio Generation: APPROVED, AUDIO_GENERATING, AUDIO_FAILED
 * - Audio Review: AUDIO_REVIEW
 * - Published: PUBLISHED
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/ENHANCEMENTS.md Section 2
 * Migration: V99__unified_library_story_status.sql
 */
enum class LibraryStoryStatus {
    // Draft phase
    /** Being edited, not submitted for review */
    DRAFT,
    
    // Translation phase
    /** Submitted for review, pipeline starting */
    SUBMITTED,
    /** Pipeline running (translation + rewrite for all languages) */
    TRANSLATING,
    /** Pipeline failed, needs retry */
    TRANSLATION_FAILED,
    
    // Content review phase
    /** All languages ready, awaiting human content review */
    CONTENT_REVIEW,
    /** Reviewer requested changes to content */
    CHANGES_REQUESTED,
    /** Story rejected by reviewer */
    REJECTED,
    
    // Audio generation phase
    /** Content approved, ready for audio generation */
    APPROVED,
    /** TTS pipeline running for all languages */
    AUDIO_GENERATING,
    /** Audio generation failed, needs retry */
    AUDIO_FAILED,
    
    // Audio review phase
    /** Audio ready for all languages, awaiting human audio review */
    AUDIO_REVIEW,
    
    // Published
    /** Live on app, visible to users */
    PUBLISHED;
    
    // Phase grouping methods
    
    fun isDraftPhase(): Boolean = this == DRAFT
    
    fun isTranslationPhase(): Boolean = when (this) {
        SUBMITTED, TRANSLATING, TRANSLATION_FAILED -> true
        else -> false
    }
    
    fun isContentReviewPhase(): Boolean = when (this) {
        CONTENT_REVIEW, CHANGES_REQUESTED, REJECTED -> true
        else -> false
    }
    
    fun isAudioGenerationPhase(): Boolean = when (this) {
        APPROVED, AUDIO_GENERATING, AUDIO_FAILED -> true
        else -> false
    }
    
    fun isAudioReviewPhase(): Boolean = this == AUDIO_REVIEW
    
    fun isPublished(): Boolean = this == PUBLISHED
    
    // State checks
    
    fun isFailedState(): Boolean = when (this) {
        TRANSLATION_FAILED, AUDIO_FAILED -> true
        else -> false
    }
    
    fun isRejectedState(): Boolean = when (this) {
        CHANGES_REQUESTED, REJECTED -> true
        else -> false
    }
    
    fun isTerminalState(): Boolean = when (this) {
        REJECTED, PUBLISHED -> true
        else -> false
    }
    
    fun isProcessingState(): Boolean = when (this) {
        TRANSLATING, AUDIO_GENERATING -> true
        else -> false
    }
    
    fun isAwaitingReview(): Boolean = when (this) {
        CONTENT_REVIEW, AUDIO_REVIEW -> true
        else -> false
    }
    
    // Workflow transition validation
    
    /**
     * Checks if transition to the new status is valid.
     * Enforces workflow state machine rules.
     */
    fun canTransitionTo(newStatus: LibraryStoryStatus): Boolean {
        return when (this) {
            DRAFT -> newStatus in setOf(SUBMITTED)
            SUBMITTED -> newStatus in setOf(TRANSLATING, DRAFT)
            TRANSLATING -> newStatus in setOf(CONTENT_REVIEW, TRANSLATION_FAILED, DRAFT)
            TRANSLATION_FAILED -> newStatus in setOf(TRANSLATING, DRAFT)
            CONTENT_REVIEW -> newStatus in setOf(APPROVED, CHANGES_REQUESTED, REJECTED)
            CHANGES_REQUESTED -> newStatus in setOf(DRAFT, REJECTED)
            REJECTED -> false // Terminal state
            APPROVED -> newStatus in setOf(AUDIO_GENERATING, CONTENT_REVIEW)
            AUDIO_GENERATING -> newStatus in setOf(AUDIO_REVIEW, AUDIO_FAILED, APPROVED)
            AUDIO_FAILED -> newStatus in setOf(AUDIO_GENERATING, APPROVED)
            AUDIO_REVIEW -> newStatus in setOf(PUBLISHED, APPROVED)
            PUBLISHED -> newStatus in setOf(DRAFT) // Allow unpublishing
        }
    }
    
    /**
     * Returns the next expected status in the happy path workflow.
     * Returns null for terminal or error states.
     */
    fun nextStatus(): LibraryStoryStatus? = when (this) {
        DRAFT -> SUBMITTED
        SUBMITTED -> TRANSLATING
        TRANSLATING -> CONTENT_REVIEW
        CONTENT_REVIEW -> APPROVED
        APPROVED -> AUDIO_GENERATING
        AUDIO_GENERATING -> AUDIO_REVIEW
        AUDIO_REVIEW -> PUBLISHED
        else -> null // Terminal, error, or rejection states
    }
    
    // Display helpers
    
    /**
     * Human-readable display name for UI.
     */
    fun displayName(): String = when (this) {
        DRAFT -> "Draft"
        SUBMITTED -> "Submitted"
        TRANSLATING -> "Translating"
        TRANSLATION_FAILED -> "Translation Failed"
        CONTENT_REVIEW -> "Content Review"
        CHANGES_REQUESTED -> "Changes Requested"
        REJECTED -> "Rejected"
        APPROVED -> "Approved"
        AUDIO_GENERATING -> "Generating Audio"
        AUDIO_FAILED -> "Audio Failed"
        AUDIO_REVIEW -> "Audio Review"
        PUBLISHED -> "Published"
    }
    
    /**
     * Description for UI tooltips or help text.
     */
    fun description(): String = when (this) {
        DRAFT -> "Story is being edited and has not been submitted for review"
        SUBMITTED -> "Story has been submitted and pipeline is starting"
        TRANSLATING -> "Translation and rewrite pipeline is running for all languages"
        TRANSLATION_FAILED -> "Translation pipeline failed and needs to be retried"
        CONTENT_REVIEW -> "All languages are ready and awaiting human content review"
        CHANGES_REQUESTED -> "Reviewer has requested changes to the story content"
        REJECTED -> "Story has been rejected and will not be published"
        APPROVED -> "Content has been approved and is ready for audio generation"
        AUDIO_GENERATING -> "Text-to-speech pipeline is running for all languages"
        AUDIO_FAILED -> "Audio generation failed and needs to be retried"
        AUDIO_REVIEW -> "Audio is ready for all languages and awaiting human review"
        PUBLISHED -> "Story is live on the app and visible to users"
    }
    
    /**
     * UI color indicator (semantic color name).
     */
    fun colorIndicator(): String = when (this) {
        DRAFT -> "gray"
        SUBMITTED -> "blue"
        TRANSLATING -> "blue"
        TRANSLATION_FAILED -> "red"
        CONTENT_REVIEW -> "yellow"
        CHANGES_REQUESTED -> "orange"
        REJECTED -> "red"
        APPROVED -> "green"
        AUDIO_GENERATING -> "blue"
        AUDIO_FAILED -> "red"
        AUDIO_REVIEW -> "yellow"
        PUBLISHED -> "green"
    }
    
    companion object {
        /**
         * Parse a string status value to enum, with fallback to DRAFT for unknown values.
         * Useful for migration and backward compatibility.
         */
        fun fromString(value: String?): LibraryStoryStatus {
            if (value == null) return DRAFT
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                // Handle legacy status values
                when (value.uppercase()) {
                    "PROCESSING" -> TRANSLATING
                    "READY" -> APPROVED
                    else -> DRAFT
                }
            }
        }
    }
}
