package com.tamixa.infrastructure.persistence

import com.tamixa.domain.visuals.StoryVisualImageStatus
import com.tamixa.domain.visuals.StoryVisualSetStatus
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
@Table(name = "story_visual_sets")
class StoryVisualSetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "story_id", nullable = false, unique = true)
    val storyId: Long,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: StoryVisualSetStatus,

    @Column(name = "source_language", nullable = false, length = 10)
    val sourceLanguage: String,

    @Column(name = "source_word_count", nullable = false)
    val sourceWordCount: Int = 0,

    @Column(name = "characters_json", columnDefinition = "TEXT")
    val charactersJson: String? = null,

    @Column(name = "character_sheet_path", length = 512)
    val characterSheetPath: String? = null,

    @Column(name = "last_error", length = 1000)
    val lastError: String? = null,

    @Column(name = "approved_at")
    val approvedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "story_visual_images")
class StoryVisualImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "set_id", nullable = false)
    val setId: Long,

    @Column(name = "image_key", nullable = false, length = 20)
    val imageKey: String,

    @Column(name = "story_moment", columnDefinition = "TEXT")
    val storyMoment: String? = null,

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    val prompt: String,

    @Column(name = "subjects_json", columnDefinition = "TEXT")
    val subjectsJson: String? = null,

    @Column(name = "image_path", length = 512)
    val imagePath: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: StoryVisualImageStatus = StoryVisualImageStatus.PENDING,

    @Column(name = "last_error", length = 1000)
    val lastError: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "story_visual_shots")
class StoryVisualShotEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "set_id", nullable = false)
    val setId: Long,

    @Column(name = "shot_index", nullable = false)
    val shotIndex: Int,

    @Column(name = "image_key", nullable = false, length = 20)
    val imageKey: String,

    @Column(name = "narration_start", nullable = false, columnDefinition = "TEXT")
    val narrationStart: String,

    @Column(name = "start_word", nullable = false)
    val startWord: Int,

    @Column(name = "camera", nullable = false, length = 20)
    val camera: String,

    @Column(name = "focus", length = 20)
    val focus: String? = null,

    @Column(name = "movement", nullable = false, length = 20)
    val movement: String,
)
