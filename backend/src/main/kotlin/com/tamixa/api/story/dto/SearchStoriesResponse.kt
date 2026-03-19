package com.tamixa.api.story.dto

data class SearchStoriesResponse(
    val content: List<SearchStoryItem>,
    val totalElements: Long
)
