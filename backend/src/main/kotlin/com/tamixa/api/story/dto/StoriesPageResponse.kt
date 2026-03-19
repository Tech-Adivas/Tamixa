package com.tamixa.api.story.dto

data class StoriesPageResponse(
    val content: List<StoryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)
