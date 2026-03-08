package com.araro.api.dto

import com.araro.domain.SoundscapeCategory

data class SoundscapeDto(
    val id: Long,
    val name: String,
    val category: SoundscapeCategory,
    val durationSeconds: Int,
    val audioUrl: String,
    val description: String?
)

data class CreateSoundscapeRequest(
    val name: String,
    val category: SoundscapeCategory,
    val description: String? = null,
    val durationSeconds: Int = 60
)

data class SoundscapeUsageDto(
    val id: Long,
    val parentId: Long,
    val storyId: Long?,
    val soundscapeId: Long,
    val soundscapeName: String,
    val usageCount: Int,
    val lastUsedAt: String
)
