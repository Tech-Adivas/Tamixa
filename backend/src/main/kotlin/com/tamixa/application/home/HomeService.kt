package com.tamixa.application.home

import com.tamixa.application.playback.PlaybackPositionEnrichedDto
import com.tamixa.application.playback.PlaybackPositionService
import com.tamixa.application.port.StoryFamilyVoiceRepositoryPort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.recommendation.RecommendedStoryDto
import com.tamixa.application.recommendation.StoryRecommendationService
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.persistence.StoryAnalyticsJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Aggregates home screen data: Continue Adventure, Recommended, Categories, Popular.
 */
@Service
class HomeService(
    private val playbackPositionService: PlaybackPositionService,
    private val recommendationService: StoryRecommendationService,
    private val storyLibraryService: StoryLibraryService,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository,
    private val appProperties: AppProperties,
    @org.springframework.beans.factory.annotation.Autowired(required = false) private val familyVoiceRepository: StoryFamilyVoiceRepositoryPort?
) {

    @Transactional(readOnly = true)
    fun getHomeData(
        parentEmail: String,
        parentId: Long,
        language: String = "ta",
        childId: Long? = null
    ): HomeResponse {
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }

        val continueAdventure = playbackPositionService.getRecentEnriched(parentId, 10)
            .map(::toContinueAdventureItem)

        val recommended = recommendationService.getRecommended(parentEmail, effectiveLang, childId, 15)
            .map(::toHomeStoryItem)

        val categories = storyLibraryService.getCategories(effectiveLang)

        val since = Instant.now().minusSeconds(60L * 60 * 24 * 30) // last 30 days
        val primaryCatalog = appProperties.translationPipeline.sourceLanguage.trim().lowercase().take(10)
        val popularIds = if (effectiveLang.equals(primaryCatalog, ignoreCase = true)) {
            storyAnalyticsJpaRepository.findPopularLibraryStoryIdsTamil(effectiveLang, since, 15)
        } else {
            storyAnalyticsJpaRepository.findPopularLibraryStoryIdsByTranslationLanguage(effectiveLang, since, 15)
        }
        val popular = if (popularIds.isEmpty()) emptyList() else {
            val listings = storyLibraryRepository.findListingByIdIn(popularIds)
            val byId = listings.associateBy { it.id }
            popularIds.mapNotNull { id -> byId[id]?.let { toPopularItem(it) } }
        }

        val familyVoiceStories = familyVoiceRepository?.let { repo ->
            val voices = repo.findByParentIdAndLanguage(parentId, effectiveLang)
            val fvIds = voices.map { it.storyId }.distinct().take(15)
            if (fvIds.isEmpty()) emptyList()
            else {
                val listings = storyLibraryRepository.findListingByIdIn(fvIds)
                listings.map { toPopularItem(it).copy(reason = "In your voice") }
            }
        } ?: emptyList()

        return HomeResponse(
            continueAdventure = continueAdventure,
            recommended = recommended,
            categories = categories,
            popular = popular,
            familyVoiceStories = familyVoiceStories
        )
    }

    private fun toContinueAdventureItem(p: PlaybackPositionEnrichedDto): HomeContinueAdventureItem =
        HomeContinueAdventureItem(
            storyId = p.storyId,
            storySource = p.storySource,
            title = p.title,
            coverImageUrl = p.coverImageUrl,
            positionSeconds = p.positionSeconds,
            progress = p.progress
        )

    private fun toHomeStoryItem(r: RecommendedStoryDto): HomeStoryItem =
        HomeStoryItem(
            storyId = r.storyId,
            storySource = r.storySource,
            title = r.title,
            theme = r.theme,
            reason = r.reason
        )

    private fun toPopularItem(listing: com.tamixa.domain.LibraryStoryListing): HomeStoryItem =
        HomeStoryItem(
            storyId = listing.id,
            storySource = "library",
            title = listing.title ?: listing.theme ?: "Story",
            theme = listing.theme ?: "",
            reason = "Popular"
        )
}

data class HomeResponse(
    val continueAdventure: List<HomeContinueAdventureItem>,
    val recommended: List<HomeStoryItem>,
    val categories: List<String>,
    val popular: List<HomeStoryItem>,
    val familyVoiceStories: List<HomeStoryItem>
)

data class HomeContinueAdventureItem(
    val storyId: Long,
    val storySource: String,
    val title: String,
    val coverImageUrl: String?,
    val positionSeconds: Int,
    val progress: Double?
)

data class HomeStoryItem(
    val storyId: Long,
    val storySource: String,
    val title: String,
    val theme: String,
    val reason: String
)
