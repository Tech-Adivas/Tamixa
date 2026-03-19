package com.tamixa.api.admin.dto

import java.time.Instant

data class StoryDetailDto(
    val id: Long,
    val parentId: Long,
    val childId: Long?,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val status: String,
    val content: String,
    val createdAt: Instant
)
