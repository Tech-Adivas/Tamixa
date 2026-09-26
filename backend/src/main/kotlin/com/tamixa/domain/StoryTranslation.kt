package com.tamixa.domain

import java.time.Instant

data class StoryTranslation(
    val id: Long,
    val masterStoryId: Long,
    val language: String,
    val title: String?,
    val content: String,
    val moral: String?,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val createdAt: Instant,
    val status: TranslationPipelineStatus = TranslationPipelineStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val narrationApprovedAt: Instant? = null,
    /** Full branching graph for this locale; when null, library master `interactive_graph` is used. */
    val interactiveGraphJson: String? = null,
    /** When null/blank at playback, [com.tamixa.domain.LibraryStory.postStoryMission] is used. */
    val postStoryMission: String? = null,
    /** When null/blank at playback, master [com.tamixa.domain.LibraryStory.postStoryResourceUrl] is used. */
    val postStoryResourceUrl: String? = null,
    /** Localized parent transparency note; falls back to master when null. */
    val parentContentNote: String? = null,
    /** Localized speak-along invitation; falls back to master when null. */
    val speakAlongPrompt: String? = null,
    /** Localized discussion prompts for parents; falls back to master when null/empty. */
    val parentDiscussionPrompts: List<String>? = null,
)
