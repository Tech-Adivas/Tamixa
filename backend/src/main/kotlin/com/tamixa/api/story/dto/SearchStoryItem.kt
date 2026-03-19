package com.tamixa.api.story.dto

data class SearchStoryItem(
    val storyId: Long,
    val storySource: String,
    val title: String?,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val coverImageUrl: String?,
    val coverVideoUrl: String? = null,
    val status: String
)
