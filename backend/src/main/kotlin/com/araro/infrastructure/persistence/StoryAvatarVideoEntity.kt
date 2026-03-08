package com.araro.infrastructure.persistence

import com.araro.domain.AvatarVideoStatus
import com.araro.domain.StoryAvatarVideo
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
@Table(name = "story_avatar_video")
class StoryAvatarVideoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "story_id", nullable = false)
    val storyId: Long,

    @Column(name = "story_source", nullable = false, length = 32)
    val storySource: String = "curated",

    @Column(name = "parent_id", nullable = false)
    val parentId: Long,

    @Column(name = "language", nullable = false, length = 16)
    val language: String = "ta",

    @Column(name = "voice_profile", nullable = false, length = 64)
    val voiceProfile: String = "default",

    @Column(name = "storage_path", length = 512)
    val storagePath: String? = null,

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    val status: AvatarVideoStatus = AvatarVideoStatus.PENDING,

    @Column(name = "replicate_prediction_id", length = 128)
    val replicatePredictionId: String? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = StoryAvatarVideo(
        id = id,
        storyId = storyId,
        storySource = storySource,
        parentId = parentId,
        language = language,
        voiceProfile = voiceProfile,
        storagePath = storagePath,
        status = status,
        replicatePredictionId = replicatePredictionId,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
