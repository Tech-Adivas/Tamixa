package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "shareable_clips")
class ShareableClipEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(name = "story_id", nullable = false)
    val storyId: Long,

    @Column(name = "story_source", nullable = false, length = 32)
    val storySource: String = "library",

    @Column(name = "language", nullable = false, length = 16)
    val language: String = "ta",

    @Column(name = "voice_profile", nullable = false, length = 64)
    val voiceProfile: String = "default",

    @Column(name = "start_seconds", nullable = false)
    val startSeconds: Int = 0,

    @Column(name = "duration_seconds", nullable = false)
    val durationSeconds: Int = 30,

    @Column(name = "format", nullable = false, length = 10)
    val format: String = "9:16",

    @Column(name = "storage_path", length = 512)
    var storagePath: String? = null,

    @Column(name = "status", nullable = false, length = 32)
    var status: String = "PENDING",

    @Column(name = "processing_job_id")
    val processingJobId: Long? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "download_analytics_emitted_at")
    var downloadAnalyticsEmittedAt: Instant? = null
)
