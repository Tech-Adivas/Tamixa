package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "curated_stories")
class CuratedStoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(length = 255)
    var title: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, length = 100)
    var theme: String,

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
    val createdAt: Instant = Instant.now()
)
