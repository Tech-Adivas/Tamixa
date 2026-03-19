package com.tamixa.api.admin

import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.domain.LibraryStory

object LibraryStoryMapper {
    /**
     * Include coverImageUrl when it's a loadable URL: http(s) or proxy path (/api/v1/covers/...).
     */
    private fun usableCoverUrl(raw: String?): String? =
        raw?.takeIf {
            it.startsWith("http://") || it.startsWith("https://") ||
                it.startsWith("/api/v1/covers/")
        }

    fun LibraryStory.toResponse(
        coverImageUrlOverride: String? = null,
        coverVideoUrlOverride: String? = null
    ): LibraryStoryResponse = LibraryStoryResponse(
        id = id,
        title = title,
        content = content,
        theme = theme,
        category = category,
        language = language,
        age = age,
        childName = childName,
        wordCount = wordCount,
        readingTimeMinutes = readingTimeMinutes,
        moral = moral,
        audioFileUrl = audioFileUrl,
        status = status,
        coverImageUrl = usableCoverUrl(coverImageUrlOverride ?: coverImageUrl),
        coverVideoUrl = usableCoverUrl(coverVideoUrlOverride ?: coverVideoUrl),
        createdAt = createdAt,
        modifiedAt = updatedAt,
        storyOwner = storyOwner,
        convertPromptUsed = convertPromptUsed,
        emotionMode = emotionMode,
        narrationApprovedAt = narrationApprovedAt,
        reviewNotes = reviewNotes,
        rejectMarkedAt = rejectMarkedAt
    )
}
