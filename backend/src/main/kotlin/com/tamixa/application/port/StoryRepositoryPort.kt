package com.tamixa.application.port

import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StoryRepositoryPort {

    fun save(story: Story): Story

    fun findById(id: Long): Story?

    fun findByParentId(parentId: Long, pageable: Pageable): Page<Story>

    fun countByParentIdAndCreatedAtBetween(parentId: Long, start: java.time.Instant, end: java.time.Instant): Long

    fun updateStatus(id: Long, status: StoryStatus)

    /** Atomic update of story content and status (e.g. MODERATION_CHECK → PENDING with final content). */
    fun updateContentAndStatus(
        id: Long,
        content: String,
        title: String?,
        moral: String?,
        wordCount: Int,
        readingTimeMinutes: Double,
        status: StoryStatus,
        safetyScore: Int? = null
    )

    fun updateAudioReady(id: Long, audioFileUrl: String)

    /** Update story content (narrated script) after TTS. Keeps display in sync with audio. */
    fun updateNarratedContent(id: Long, content: String, wordCount: Int, readingTimeMinutes: Double)

    fun updateCoverImage(id: Long, coverImageUrl: String)

    /** Update cover image and optional cover video (animated GIF) for a generated story. */
    fun updateCover(id: Long, coverImageUrl: String, coverVideoUrl: String? = null)

    /** Search parent's stories by theme or title (case-insensitive). */
    fun searchByParentId(parentId: Long, query: String, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Story>
}
