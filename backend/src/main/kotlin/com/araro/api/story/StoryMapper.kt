package com.araro.api.story

import com.araro.api.story.dto.StoryResponse
import com.araro.domain.Story

fun Story.toResponse(coverImageUrlOverride: String? = null): StoryResponse = StoryResponse(
    id = id,
    parentId = parentId,
    childId = childId,
    content = content,
    theme = theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    title = title,
    moral = moral,
    status = status,
    audioFileUrl = audioFileUrl,
    createdAt = createdAt,
    coverImageUrl = coverImageUrlOverride ?: coverImageUrl
)
