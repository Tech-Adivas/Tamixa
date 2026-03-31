package com.tamixa.domain

import java.time.Instant

data class ParentAvatar(
    val id: Long,
    val parentId: Long,
    val storagePath: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val heygenTalkingPhotoId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
