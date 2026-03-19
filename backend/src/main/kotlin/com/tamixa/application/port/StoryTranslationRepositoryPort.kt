package com.tamixa.application.port

import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.StoryTranslationListing
import com.tamixa.domain.TranslationPipelineStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StoryTranslationRepositoryPort {

    fun save(translation: StoryTranslation): StoryTranslation

    fun findById(id: Long): StoryTranslation?

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryTranslation?

    fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean

    fun findByLanguage(language: String, pageable: Pageable): Page<StoryTranslation>

    fun findByLanguageAndMasterNarrationApproved(language: String, pageable: Pageable): Page<StoryTranslation>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<StoryTranslationListing>

    fun findListingByLanguageAndMasterNarrationApproved(language: String, pageable: Pageable): Page<StoryTranslationListing>

    fun findListingByLanguageAndMasterNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<StoryTranslationListing>

    fun findByLanguageAndMasterNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<StoryTranslation>

    fun findByMasterStoryId(masterStoryId: Long): List<StoryTranslation>

    fun findByStatusIn(statuses: List<TranslationPipelineStatus>): List<StoryTranslation>

    fun atomicStatusUpdate(id: Long, newStatus: TranslationPipelineStatus, lastError: String?): Boolean

    fun incrementRetryCount(id: Long, lastError: String): Boolean

    fun resetRetryCountByMasterStoryId(masterStoryId: Long): Int

    fun resetRetryCountByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Int

    /** Delete all translations for a master story (cascade deletes narration audio). Used when story content is edited. */
    fun deleteByMasterStoryId(masterStoryId: Long)

    /** Set narration approved timestamp for a translation (per-language approval). */
    fun updateNarrationApprovedAt(masterStoryId: Long, language: String, approvedAt: java.time.Instant?)

    /** Returns map of language -> approved (true if narration_approved_at is set) for a story. */
    fun getNarrationApprovalByMasterStoryId(masterStoryId: Long): Map<String, Boolean>
}
