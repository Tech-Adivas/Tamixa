package com.tamixa.infrastructure.persistence

import com.tamixa.domain.StoryStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stories")
class StoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    val child: ChildEntity? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, length = 100)
    val theme: String,

    @Column(nullable = false, length = 10)
    val language: String,

    @Column(nullable = false)
    val age: Int,

    @Column(name = "child_name", nullable = false, length = 255)
    val childName: String,

    @Column(name = "word_count", nullable = false)
    var wordCount: Int,

    @Column(name = "reading_time_minutes", nullable = false)
    var readingTimeMinutes: Double,

    @Column(length = 255)
    var title: String? = null,

    @Column(columnDefinition = "TEXT")
    var moral: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StoryStatus = StoryStatus.PENDING,

    @Column(name = "audio_file_url", length = 512)
    var audioFileUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "safety_score")
    var safetyScore: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_profile_id")
    val voiceProfile: VoiceProfileEntity? = null,

    @Column(name = "emotion_mode", length = 20)
    val emotionMode: String? = null,

    @Column(name = "parent_custom_prompt", columnDefinition = "TEXT")
    val parentCustomPrompt: String? = null,

    @Column(name = "cover_image_url", length = 512)
    var coverImageUrl: String? = null,

    @Column(name = "cover_video_url", length = 512)
    var coverVideoUrl: String? = null
)
