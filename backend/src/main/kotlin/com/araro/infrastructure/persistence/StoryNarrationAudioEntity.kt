package com.araro.infrastructure.persistence

import com.araro.domain.narration.NarrationAudioStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "story_narration_audio",
    uniqueConstraints = [UniqueConstraint(columnNames = ["translation_id", "voice_profile"])]
)
class StoryNarrationAudioEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "translation_id", nullable = false)
    val translationId: Long,

    @Column(name = "voice_profile", nullable = false, length = 100)
    val voiceProfile: String = "default",

    @Column(name = "audio_url", nullable = false, length = 512)
    val audioUrl: String,

    @Column(name = "duration_seconds", nullable = false)
    val durationSeconds: Int = 0,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: NarrationAudioStatus = NarrationAudioStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
