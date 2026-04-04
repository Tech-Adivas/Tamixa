package com.tamixa.api.dto

import com.tamixa.domain.VoiceCloningStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VoiceCloningJobDto(
    val id: Long,
    val parentId: Long,
    val audioStoragePath: String,
    val audioFileSizeBytes: Long,
    val voiceName: String,
    val elevenLabsVoiceId: String?,
    val fishAudioModelId: String? = null,
    val status: VoiceCloningStatus,
    val errorMessage: String?,
    val createdAt: String,
    val completedAt: String?
)

data class CreateVoiceCloningJobRequest(
    @field:NotBlank(message = "Audio storage path is required")
    @field:Size(max = 1024, message = "Audio storage path must not exceed 1024 characters")
    val audioStoragePath: String,

    @field:Size(max = 1024, message = "Consent audio path must not exceed 1024 characters")
    val consentAudioStoragePath: String? = null,

    val audioFileSizeBytes: Long,

    @field:NotBlank(message = "Voice name is required")
    @field:Size(max = 100, message = "Voice name must not exceed 100 characters")
    val voiceName: String
)

data class VoiceTierDto(
    val id: Long,
    val name: String,
    val priceMonthly: Long,
    val priceYearly: Long,
    val maxChildren: Int,
    val maxVoices: Int,
    val maxAvatarVideos: Int,
    val maxSoundscapes: Int,
    val allowsVoiceCloning: Boolean,
    val allowsAvatarVideo: Boolean,
    val allowsSoundscapes: Boolean,
    val allowsFamilySharing: Boolean,
    val analyticsEnabled: Boolean
)
