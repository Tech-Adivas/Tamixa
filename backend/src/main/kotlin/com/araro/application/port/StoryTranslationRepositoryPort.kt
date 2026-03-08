package com.araro.application.port

import com.araro.domain.StoryTranslation
import com.araro.domain.StoryTranslationListing
import com.araro.domain.TranslationPipelineStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StoryTranslationRepositoryPort {

    fun save(translation: StoryTranslation): StoryTranslation

    fun findById(id: Long): StoryTranslation?

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryTranslation?

    fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean

    fun findByLanguage(language: String, pageable: Pageable): Page<StoryTranslation>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<StoryTranslationListing>

    fun findByMasterStoryId(masterStoryId: Long): List<StoryTranslation>

    fun findByStatusIn(statuses: List<TranslationPipelineStatus>): List<StoryTranslation>

    fun atomicStatusUpdate(id: Long, newStatus: TranslationPipelineStatus, lastError: String?): Boolean

    fun incrementRetryCount(id: Long, lastError: String): Boolean

    fun resetRetryCountByMasterStoryId(masterStoryId: Long): Int

    fun resetRetryCountByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Int

    /** Delete all translations for a master story (cascade deletes narration audio). Used when story content is edited. */
    fun deleteByMasterStoryId(masterStoryId: Long)
}
