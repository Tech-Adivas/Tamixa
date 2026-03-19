package com.tamixa.api.story

import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.api.story.dto.StoryResponse
import com.tamixa.domain.Story

fun Story.toResponse(coverImageUrlOverride: String? = null, coverVideoUrlOverride: String? = null): StoryResponse = StoryResponse(
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
    coverImageUrl = coverImageUrlOverride ?: coverImageUrl,
    coverVideoUrl = coverVideoUrlOverride
)

/** Resolves both cover image and video URLs via CoverImageUrlResolver. */
fun Story.toResponse(resolver: CoverImageUrlResolver): StoryResponse = toResponse(
    coverImageUrlOverride = resolver.resolveCoverPath(coverImageUrl),
    coverVideoUrlOverride = resolver.resolveCoverVideoPath(coverVideoUrl)
)
