package com.araro.application.port

import com.araro.domain.CuratedStory
import com.araro.domain.CuratedStoryListing
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CuratedStoryRepositoryPort {

    fun save(story: CuratedStory): CuratedStory

    fun findById(id: Long): CuratedStory?

    fun findAll(pageable: Pageable): Page<CuratedStory>

    fun findAllWithProcessingFirst(pageable: Pageable): Page<CuratedStory>

    fun findByStatusInWithProcessingFirst(statuses: List<String>, pageable: Pageable): Page<CuratedStory>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<CuratedStoryListing>

    fun findListingByIdIn(ids: List<Long>): List<CuratedStoryListing>

    fun findByLanguage(language: String, pageable: Pageable): Page<CuratedStory>

    fun findByStatus(status: String, pageable: Pageable): Page<CuratedStory>

    fun findByStatusIn(statuses: List<String>, pageable: Pageable): Page<CuratedStory>

    fun existsByTitle(title: String): Boolean

    fun updateAudioUrl(id: Long, audioFileUrl: String)

    fun updateStatus(id: Long, status: String)

    fun updateStatusBulk(ids: List<Long>, status: String): Int

    fun updateThemeBulk(ids: List<Long>, theme: String): Int

    fun update(story: CuratedStory): CuratedStory

    /** Update content, wordCount, readingTimeMinutes (e.g. after pipeline produces conversational script). */
    fun updateContent(id: Long, content: String, wordCount: Int, readingTimeMinutes: Double)

    /** Clear audio URL when invalidating translations for content edit. */
    fun clearAudioUrl(id: Long)

    /** Clear audio_file_url for all curated stories (legacy audio removal). */
    fun clearAllAudioUrls(): Int

    /** Search by theme or title (case-insensitive). */
    fun searchByThemeOrTitle(query: String, language: String, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<CuratedStory>

    /** Delete curated story by id. Caller must clean up related data (favorites, analytics, etc.) first. */
    fun deleteById(id: Long)
}
