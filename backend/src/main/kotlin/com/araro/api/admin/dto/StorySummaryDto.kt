package com.araro.api.admin.dto

import java.time.Instant

data class StorySummaryDto(
    val id: Long,
    val parentId: Long,
    val childId: Long?,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val status: String,
    val safetyScore: Int?,
    val createdAt: Instant
)
