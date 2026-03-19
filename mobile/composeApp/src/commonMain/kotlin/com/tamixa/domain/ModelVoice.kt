package com.tamixa.domain

import kotlinx.serialization.Serializable

@Serializable
data class VoiceProfile(
    val id: Long,
    val parentId: Long,
    val createdAt: String
)
