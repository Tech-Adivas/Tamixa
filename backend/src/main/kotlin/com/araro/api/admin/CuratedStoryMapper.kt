package com.araro.api.admin

import com.araro.api.admin.dto.CuratedStoryResponse
import com.araro.domain.CuratedStory

object CuratedStoryMapper {
    /**
     * Include coverImageUrl when it's a loadable URL: http(s) or proxy path (/api/v1/covers/...).
     */
    private fun usableCoverUrl(raw: String?): String? =
        raw?.takeIf {
            it.startsWith("http://") || it.startsWith("https://") ||
                it.startsWith("/api/v1/covers/")
        }

    fun CuratedStory.toResponse(
        coverImageUrlOverride: String? = null,
        coverVideoUrlOverride: String? = null
    ): CuratedStoryResponse = CuratedStoryResponse(
        id = id,
        title = title,
        content = content,
        theme = theme,
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
        emotionMode = emotionMode
    )
}
