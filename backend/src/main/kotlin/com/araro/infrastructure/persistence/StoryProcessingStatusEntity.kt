package com.araro.infrastructure.persistence

import com.araro.domain.ProcessingStage
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
@Table(name = "story_processing_status")
class StoryProcessingStatusEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "master_story_id", nullable = false)
    val masterStoryId: Long,

    @Column(nullable = false, length = 10)
    val language: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var stage: ProcessingStage,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
