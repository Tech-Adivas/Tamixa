package com.tamixa.application.port

import com.tamixa.domain.LibraryStory
import com.tamixa.domain.LibraryStoryListing
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StoryLibraryRepositoryPort {

    fun save(story: LibraryStory): LibraryStory

    fun findById(id: Long): LibraryStory?

    fun findAll(pageable: Pageable): Page<LibraryStory>

    fun findAllWithProcessingFirst(pageable: Pageable): Page<LibraryStory>

    fun findByStatusInWithProcessingFirst(statuses: List<String>, pageable: Pageable): Page<LibraryStory>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<LibraryStoryListing>

    fun findListingByLanguageAndNarrationApproved(language: String, pageable: Pageable): Page<LibraryStoryListing>

    fun findListingByLanguageAndNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<LibraryStoryListing>

    fun findDistinctThemesByNarrationApproved(language: String): List<String>

    /** Distinct themes for masters with approved translation in given language (non-Tamil). */
    fun findDistinctThemesByNarrationApprovedAndTranslationLanguage(language: String): List<String>

    fun findListingByIdIn(ids: List<Long>): List<LibraryStoryListing>

    fun findByLanguage(language: String, pageable: Pageable): Page<LibraryStory>

    fun findByLanguageAndNarrationApproved(language: String, pageable: Pageable): Page<LibraryStory>

    fun findByLanguageAndNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<LibraryStory>

    fun findByStatus(status: String, pageable: Pageable): Page<LibraryStory>

    fun findByStatusPublishedAndNarrationApprovedAtNull(pageable: Pageable): Page<LibraryStory>

    /** Approved stories for Story to Speech (trigger TTS after approval). */
    fun findByNarrationApprovedAtNotNull(pageable: Pageable): Page<LibraryStory>

    fun findByStatusIn(statuses: List<String>, pageable: Pageable): Page<LibraryStory>

    fun existsByTitle(title: String): Boolean

    fun updateAudioUrl(id: Long, audioFileUrl: String)

    fun updateStatus(id: Long, status: String)

    fun updateStatusAndReviewNotes(id: Long, status: String, reviewNotes: String?)

    fun updateStatusBulk(ids: List<Long>, status: String): Int

    fun updateThemeBulk(ids: List<Long>, theme: String): Int

    fun update(story: LibraryStory): LibraryStory

    /** Update content, wordCount, readingTimeMinutes (e.g. after pipeline produces conversational script). */
    fun updateContent(id: Long, content: String, wordCount: Int, readingTimeMinutes: Double)

    /** Clear audio URL when invalidating translations for content edit. */
    fun clearAudioUrl(id: Long)

    /** Clear audio_file_url for all library stories (legacy audio removal). */
    fun clearAllAudioUrls(): Int

    /** Search by theme or title (case-insensitive). */
    fun searchByThemeOrTitle(query: String, language: String, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<LibraryStory>

    /** Delete library story by id. Caller must clean up related data (favorites, analytics, etc.) first. */
    fun deleteById(id: Long)

    /** Set when narration was approved for final delivery by a human. */
    fun updateNarrationApprovedAt(id: Long, approvedAt: java.time.Instant?)

    /** Set when admin has marked story for reject from the language view. */
    fun updateRejectMarkedAt(id: Long, markedAt: java.time.Instant?)
}
