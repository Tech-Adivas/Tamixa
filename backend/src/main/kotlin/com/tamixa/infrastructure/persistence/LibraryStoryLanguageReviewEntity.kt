package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "library_story_language_reviews")
class LibraryStoryLanguageReviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "library_story_id", nullable = false)
    val libraryStoryId: Long,

    @Column(nullable = false, length = 10)
    val language: String,

    @Column(name = "reviewed_at", nullable = false)
    val reviewedAt: Instant = Instant.now()
)
