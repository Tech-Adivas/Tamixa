package com.tamixa.domain

data class FavoriteStory(
    val id: Long,
    val parentId: Long,
    val storyId: Long,
    val storySource: String,
    val childId: Long?,
    val createdAt: java.time.Instant
)
