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
@Table(name = "story_voice_preference")
class StoryVoicePreferenceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,
    @Column(name = "story_id", nullable = false)
    val storyId: Long,
    @Column(name = "story_source", nullable = false, length = 20)
    val storySource: String = "library",
    @Column(name = "voice_profile", nullable = false, length = 100)
    var voiceProfile: String = "default",
    @Column(name = "playback_mode", nullable = false, length = 20)
    var playbackMode: String = "default",
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
