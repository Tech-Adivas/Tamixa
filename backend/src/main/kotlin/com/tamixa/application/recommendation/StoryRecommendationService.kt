package com.tamixa.application.recommendation

import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.FavoriteStoryRepositoryPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.infrastructure.config.AppProperties
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Story recommendations based on favorites, child interests, and age.
 */
@Service
class StoryRecommendationService(
    private val parentRepository: ParentRepositoryPort,
    private val favoriteRepository: FavoriteStoryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyTranslationRepository: StoryTranslationRepositoryPort,
    private val appProperties: AppProperties
) {

    @Transactional(readOnly = true)
    fun getRecommended(
        parentEmail: String,
        language: String = "ta",
        childId: Long? = null,
        limit: Int = 15
    ): List<RecommendedStoryDto> {
        val parent = parentRepository.findByEmail(parentEmail) ?: return emptyList()
        val favorites = favoriteRepository.findByParentId(parent.id).map { it.storyId to it.storySource }.toSet()
        val interests = emptyList<String>() // Child feature removed
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }

        val primaryCatalog = appProperties.translationPipeline.sourceLanguage.trim().lowercase().take(10)
        val library = if (effectiveLang.equals(primaryCatalog, ignoreCase = true)) {
            storyLibraryRepository.findByLanguageAndNarrationApproved(effectiveLang, PageRequest.of(0, 30)).content
        } else {
            val pageable = PageRequest.of(0, 30)
            val translations = (
                if (appProperties.translationPipeline.masterOnlyNarration) {
                    storyTranslationRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudio(effectiveLang, pageable)
                } else {
                    storyTranslationRepository.findByLanguageAndMasterNarrationApproved(effectiveLang, pageable)
                }
                ).content
            val masterIds = translations.map { it.masterStoryId }.distinct()
            if (masterIds.isEmpty()) emptyList()
            else {
                val translationByMaster = translations.associateBy { it.masterStoryId }
                storyLibraryRepository.findListingByIdIn(masterIds).map { listing ->
                    val translation = translationByMaster[listing.id]
                    val displayTitle = translation?.title?.takeIf { it.isNotBlank() } ?: listing.title ?: listing.theme ?: ""
                    com.tamixa.domain.LibraryStory(
                        id = listing.id,
                        title = displayTitle,
                        content = "",
                        theme = listing.theme ?: "",
                        language = effectiveLang,
                        age = listing.age,
                        childName = listing.childName ?: "Child",
                        wordCount = listing.wordCount,
                        readingTimeMinutes = listing.readingTimeMinutes,
                        moral = null,
                        audioFileUrl = listing.audioFileUrl,
                        status = com.tamixa.domain.LibraryStoryStatus.fromString(listing.status),
                        coverImageUrl = listing.coverImageUrl,
                        coverVideoUrl = listing.coverVideoUrl,
                        createdAt = listing.createdAt,
                        updatedAt = listing.createdAt,
                        storyOwner = null,
                        convertPromptUsed = null,
                        emotionMode = null,
                        narrationApprovedAt = null
                    )
                }
            }
        }
        val generated = storyRepository.findByParentId(parent.id, PageRequest.of(0, 20)).content

        val results = mutableListOf<RecommendedStoryDto>()

        library.forEach { c ->
            val theme = (c.title ?: c.theme).lowercase()
            val score = when {
                (c.id to "library") in favorites -> 3
                interests.any { theme.contains(it) } -> 2
                else -> 1
            }
            results.add(
                RecommendedStoryDto(
                    storyId = c.id,
                    storySource = "library",
                    title = c.title ?: c.theme,
                    theme = c.theme,
                    age = c.age,
                    reason = when (score) {
                        3 -> "In your favorites"
                        2 -> "Matches interests"
                        else -> "Recommended"
                    }
                )
            )
        }

        generated.filter { it.status.name == "READY" || it.status.name == "PENDING" }.forEach { s ->
            val theme = (s.title ?: s.theme).lowercase()
            val score = when {
                (s.id to "generated") in favorites -> 3
                interests.any { theme.contains(it) } -> 2
                else -> 1
            }
            results.add(
                RecommendedStoryDto(
                    storyId = s.id,
                    storySource = "generated",
                    title = s.title ?: s.theme,
                    theme = s.theme,
                    age = s.age,
                    reason = when (score) {
                        3 -> "In your favorites"
                        2 -> "Matches interests"
                        else -> "Your story"
                    }
                )
            )
        }

        return results
            .distinctBy { it.storyId to it.storySource }
            .sortedByDescending { when (it.reason) {
                "In your favorites" -> 3
                "Matches interests" -> 2
                else -> 1
            } }
            .take(limit.coerceIn(1, 50))
    }
}

data class RecommendedStoryDto(
    val storyId: Long,
    val storySource: String,
    val title: String,
    val theme: String,
    val age: Int,
    val reason: String
)
