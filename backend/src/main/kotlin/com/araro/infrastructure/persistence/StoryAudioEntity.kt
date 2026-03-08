package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_audio")
class StoryAudioEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "master_story_id", nullable = false)
    val masterStoryId: Long,

    @Column(nullable = false, length = 10)
    val language: String,

    @Column(name = "audio_file_url", nullable = false, length = 512)
    val audioFileUrl: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
