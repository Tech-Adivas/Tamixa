package com.araro.infrastructure.persistence

import com.araro.domain.TranslationPipelineStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    var lastError: String? = null
)
