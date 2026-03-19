package com.tamixa.application.storylibrary

/**
 * Story that has at least one translation with failed pipeline status.
 * For admin "Stories with issues" menu.
 */
data class StoryWithIssues(
    val storyId: Long,
    val title: String,
    val theme: String,
    val status: String,
    val issues: List<LanguageIssue>
)

data class LanguageIssue(
    val language: String,
    val status: String,
    val error: String
)
