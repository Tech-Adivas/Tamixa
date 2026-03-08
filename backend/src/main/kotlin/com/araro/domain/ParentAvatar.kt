package com.araro.domain

import java.time.Instant

data class ParentAvatar(
    val id: Long,
    val parentId: Long,
    val storagePath: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)
