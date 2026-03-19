package com.tamixa.infrastructure.persistence

import com.tamixa.domain.StoryFamilyVoice
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_family_voice")
class StoryFamilyVoiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "story_id", nullable = false)
    val storyId: Long,

    @Column(name = "parent_id", nullable = false)
    val parentId: Long,

    @Column(name = "language", nullable = false, length = 16)
    val language: String,

    @Column(name = "storage_path", nullable = false, length = 512)
    val storagePath: String,

    @Column(name = "file_size_bytes", nullable = false)
    val fileSizeBytes: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = StoryFamilyVoice(
        id = id,
        storyId = storyId,
        parentId = parentId,
        language = language,
        storagePath = storagePath,
        fileSizeBytes = fileSizeBytes,
        createdAt = createdAt
    )

    companion object {
        fun from(domain: StoryFamilyVoice) = StoryFamilyVoiceEntity(
            id = domain.id,
            storyId = domain.storyId,
            parentId = domain.parentId,
            language = domain.language,
            storagePath = domain.storagePath,
            fileSizeBytes = domain.fileSizeBytes,
            createdAt = domain.createdAt
        )
    }
}
