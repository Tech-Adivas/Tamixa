package com.araro.api.dto

import com.araro.domain.VoiceCloningStatus

data class VoiceCloningJobDto(
    val id: Long,
    val parentId: Long,
    val audioStoragePath: String,
    val audioFileSizeBytes: Long,
    val voiceName: String,
    val elevenLabsVoiceId: String?,
    val status: VoiceCloningStatus,
    val errorMessage: String?,
    val createdAt: String,
    val completedAt: String?
)

data class CreateVoiceCloningJobRequest(
    val audioStoragePath: String,
    val audioFileSizeBytes: Long,
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
