package com.araro.application.recommendation

import com.araro.application.port.ChildRepositoryPort
import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.FavoriteStoryRepositoryPort
import com.araro.application.port.ParentRepositoryPort
import com.araro.application.port.StoryRepositoryPort
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
    private val curatedRepository: CuratedStoryRepositoryPort,
    private val childRepository: ChildRepositoryPort
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
        val interests = childId?.let { childRepository.findById(it)?.interests?.lowercase()?.split(",")?.map { w -> w.trim() }?.filter { it.isNotBlank() } } ?: emptyList()

        val curated = curatedRepository.findByLanguage(language.trim().lowercase().take(10).ifEmpty { "ta" }, PageRequest.of(0, 30)).content
        val generated = storyRepository.findByParentId(parent.id, PageRequest.of(0, 20)).content

        val results = mutableListOf<RecommendedStoryDto>()

        curated.forEach { c ->
            val theme = (c.title ?: c.theme).lowercase()
            val score = when {
                (c.id to "curated") in favorites -> 3
                interests.any { theme.contains(it) } -> 2
                else -> 1
            }
            results.add(
                RecommendedStoryDto(
                    storyId = c.id,
                    storySource = "curated",
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
