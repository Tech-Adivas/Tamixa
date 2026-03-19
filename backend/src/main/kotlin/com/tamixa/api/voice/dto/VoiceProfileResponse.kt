package com.tamixa.api.voice.dto

import java.time.Instant

data class VoiceProfileResponse(
    val id: Long,
    val parentId: Long,
    val createdAt: Instant,
    val heygenVoiceId: String? = null
)
