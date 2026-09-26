package com.tamixa.infrastructure.persistence

import com.tamixa.domain.TranslationPipelineStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "story_translations")
class StoryTranslationEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "master_story_id", nullable = false)
    val masterStoryId: Long,

    @Column(nullable = false, length = 10)
    val language: String,

    @Column(length = 255)
    val title: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(columnDefinition = "TEXT")
    val moral: String? = null,

    @Column(name = "word_count", nullable = false)
    val wordCount: Int = 0,

    @Column(name = "reading_time_minutes", nullable = false)
    val readingTimeMinutes: Double = 0.0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TranslationPipelineStatus = TranslationPipelineStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "narration_approved_at")
    var narrationApprovedAt: java.time.Instant? = null,

    @Column(name = "interactive_graph", columnDefinition = "TEXT")
    var interactiveGraph: String? = null,

    @Column(name = "post_story_mission", columnDefinition = "TEXT")
    var postStoryMission: String? = null,

    @Column(name = "post_story_resource_url", length = 512)
    var postStoryResourceUrl: String? = null,

    @Column(name = "parent_content_note", columnDefinition = "TEXT")
    var parentContentNote: String? = null,

    @Column(name = "speak_along_prompt", length = 500)
    var speakAlongPrompt: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parent_discussion_prompts", columnDefinition = "jsonb")
    var parentDiscussionPrompts: List<String>? = null,
)
