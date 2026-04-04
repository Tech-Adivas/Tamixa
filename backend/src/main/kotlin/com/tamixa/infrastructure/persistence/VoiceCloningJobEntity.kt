package com.tamixa.infrastructure.persistence

import com.tamixa.domain.VoiceCloningStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "voice_cloning_jobs")
data class VoiceCloningJobEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val parentId: Long,

    @Column(nullable = false, length = 500)
    val audioStoragePath: String,

    @Column(length = 500)
    val consentAudioStoragePath: String? = null,

    @Column(nullable = false)
    val audioFileSizeBytes: Long,

    @Column(nullable = false, length = 255)
    val voiceName: String,

    @Column(length = 255)
    val elevenLabsVoiceId: String?,

    @Column(length = 128)
    val fishAudioModelId: String? = null,

    @Column(columnDefinition = "TEXT")
    val googleVoiceCloningKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val status: VoiceCloningStatus,

    @Column(length = 1000)
    val errorMessage: String?,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column
    val completedAt: Instant?
)
