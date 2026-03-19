package com.tamixa.domain

import java.time.Instant

/**
 * Voice cloning job: parent uploads audio, system creates voice profile.
 * Status: PENDING → PROCESSING → READY | FAILED
 */
data class VoiceCloningJob(
    val id: Long,
    val parentId: Long,
    val audioStoragePath: String,
    val consentAudioStoragePath: String? = null,
    val audioFileSizeBytes: Long,
    val voiceName: String,
    val elevenLabsVoiceId: String?,
    val googleVoiceCloningKey: String? = null,
    val status: VoiceCloningStatus,
    val errorMessage: String?,
    val createdAt: Instant,
    val completedAt: Instant?
)

enum class VoiceCloningStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
