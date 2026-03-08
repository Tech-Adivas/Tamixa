package com.araro.api.voice.dto

import java.time.Instant

data class VoiceProfileResponse(
    val id: Long,
    val parentId: Long,
    val createdAt: Instant
)
