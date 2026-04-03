package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "library_stories")
class LibraryStoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(length = 255)
    var title: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, length = 100)
    var theme: String,

    @Column(length = 100)
    var category: String? = null,

    @Column(nullable = false, length = 10)
    val language: String = "ta",

    @Column(nullable = false)
    val age: Int,

    @Column(name = "child_name", nullable = false, length = 255)
    val childName: String = "Child",

    @Column(name = "word_count", nullable = false)
    var wordCount: Int = 0,

    @Column(name = "reading_time_minutes", nullable = false)
    var readingTimeMinutes: Double = 0.0,

    @Column(columnDefinition = "TEXT")
    var moral: String? = null,

    @Column(name = "audio_file_url", length = 512)
    var audioFileUrl: String? = null,

    @Column(nullable = false, length = 20)
    var status: String = "DRAFT",

    @Column(name = "cover_image_url", length = 512)
    var coverImageUrl: String? = null,

    @Column(name = "cover_video_url", length = 512)
    var coverVideoUrl: String? = null,

    @Column(name = "emotion_mode", length = 20)
    var emotionMode: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "story_owner", length = 255)
    var storyOwner: String? = null,

    @Column(name = "convert_prompt_used", columnDefinition = "TEXT")
    var convertPromptUsed: String? = null,

    @Column(name = "narration_approved_at")
    var narrationApprovedAt: java.time.Instant? = null,

    @Column(name = "review_notes", columnDefinition = "TEXT")
    var reviewNotes: String? = null,

    @Column(name = "reject_marked_at")
    var rejectMarkedAt: java.time.Instant? = null,

    @Column(name = "regenerate_prompt_locked", nullable = false)
    var regeneratePromptLocked: Boolean = false,

    @Column(name = "regenerate_prompt_lock_approved", nullable = false)
    var regeneratePromptLockApproved: Boolean = false,

    @Column(name = "regenerate_prompt_unlock_requested_at")
    var regeneratePromptUnlockRequestedAt: java.time.Instant? = null,

    /** When set, row is excluded from listings; purge job deletes permanently after retention. */
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parent_discussion_prompts", columnDefinition = "jsonb")
    var parentDiscussionPrompts: List<String>? = null,

    @Column(name = "parent_content_note", columnDefinition = "TEXT")
    var parentContentNote: String? = null,

    @Column(name = "speak_along_prompt", length = 500)
    var speakAlongPrompt: String? = null,
)
