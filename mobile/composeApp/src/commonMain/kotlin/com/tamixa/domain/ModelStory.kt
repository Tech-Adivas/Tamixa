package com.tamixa.domain

import kotlinx.serialization.Serializable

@Serializable
enum class StoryStatus { PENDING, PENDING_REVIEW, GENERATING, READY, FAILED }

@Serializable
data class Story(
    val id: Long,
    val parentId: Long,
    val childId: Long?,
    val content: String,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val title: String? = null,
    val moral: String? = null,
    val status: StoryStatus = StoryStatus.PENDING,
    val audioFileUrl: String? = null,
    val coverImageUrl: String? = null,
    val coverVideoUrl: String? = null,
    val createdAt: String,
    /** Curated story category from API; used for dashboard filter. */
    val category: String? = null
)

/** Paginated response from GET /stories/library. */
@Serializable
data class LibraryStoriesPageResponse(
    val content: List<LibraryStoryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

/** Response from GET /stories/curated - curated stories. content omitted in listing; use empty when missing. */
@Serializable
data class LibraryStoryResponse(
    val id: Long,
    val title: String? = null,
    val content: String = "",
    val theme: String,
    val category: String? = null,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val moral: String? = null,
    val audioFileUrl: String? = null,
    val coverImageUrl: String? = null,
    val coverVideoUrl: String? = null,
    val createdAt: String
)

fun LibraryStoryResponse.toStory(): Story = Story(
    id = id,
    parentId = 0L,  // Sentinel: curated stories have no parent
    childId = null,
    content = content,
    theme = title?.takeIf { it.isNotBlank() } ?: theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    title = title?.takeIf { it.isNotBlank() },
    moral = moral,
    status = StoryStatus.READY,
    audioFileUrl = audioFileUrl,
    coverImageUrl = coverImageUrl,
    coverVideoUrl = coverVideoUrl,
    createdAt = createdAt,
    category = category?.trim()?.takeIf { it.isNotBlank() }
)

/** Response from GET /stories (parent's generated stories). */
@Serializable
data class StoryApiResponse(
    val id: Long,
    val parentId: Long,
    val childId: Long?,
    val content: String,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val title: String? = null,
    val moral: String? = null,
    val status: String,
    val audioFileUrl: String? = null,
    val coverImageUrl: String? = null,
    val coverVideoUrl: String? = null,
    val createdAt: String
)

@Serializable
data class StoriesPageResponse(
    val content: List<StoryApiResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

fun StoryApiResponse.toStory(): Story = Story(
    id = id,
    parentId = parentId,
    childId = childId,
    content = content,
    theme = title?.takeIf { it.isNotBlank() } ?: theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    title = title,
    moral = moral,
    status = runCatching { StoryStatus.valueOf(status) }.getOrDefault(StoryStatus.PENDING),
    audioFileUrl = audioFileUrl,
    coverImageUrl = coverImageUrl,
    coverVideoUrl = coverVideoUrl,
    createdAt = createdAt
)

@Serializable
data class GenerateStoryRequest(
    val age: Int,
    val language: String = "ta",
    val theme: String,
    val childName: String = "Listener",
    val childId: Long? = null,
    /** Phase 2: CALM, SOOTHING, ADVENTUROUS, DEFAULT. Influences tone (e.g. bedtime). */
    val emotionMode: String? = null,
    /** Phase 2: Parent instructions (e.g. "include a puppy", "set in a forest"). */
    val parentCustomPrompt: String? = null,
    /** Conversation messages (feelings, preferences) to tune the prompt. */
    val conversationMessages: List<String>? = null,
    /** Phase 2: Parent's voice profile ID for cloned TTS. */
    val voiceProfileId: Long? = null
)
