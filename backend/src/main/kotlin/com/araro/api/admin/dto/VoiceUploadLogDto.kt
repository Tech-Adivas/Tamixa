package com.araro.api.admin.dto

import java.time.Instant

data class VoiceUploadLogDto(
    val id: Long,
    val parentId: Long,
    val parentEmail: String?,
    val createdAt: Instant
)
