package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_versions")
class StoryVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "library_story_id", nullable = false)
    val libraryStoryId: Long,

    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,

    @Column(length = 255)
    val title: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false, length = 100)
    val theme: String,

    @Column(columnDefinition = "TEXT")
    val moral: String? = null,

    @Column(name = "word_count", nullable = false)
    val wordCount: Int = 0,

    @Column(name = "reading_time_minutes", nullable = false)
    val readingTimeMinutes: Double = 0.0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "created_by", length = 255)
    val createdBy: String? = null
) {
    fun toDomain() = com.tamixa.domain.StoryVersion(
        id = id,
        libraryStoryId = libraryStoryId,
        versionNumber = versionNumber,
        title = title,
        content = content,
        theme = theme,
        moral = moral,
        wordCount = wordCount,
        readingTimeMinutes = readingTimeMinutes,
        createdAt = createdAt,
        createdBy = createdBy
    )
}
