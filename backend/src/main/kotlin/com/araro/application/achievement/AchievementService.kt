package com.araro.application.achievement

import com.araro.infrastructure.persistence.ChildAchievementEntity
import com.araro.infrastructure.persistence.ChildAchievementJpaRepository
import com.araro.infrastructure.persistence.ChildJpaRepository
import com.araro.infrastructure.persistence.StoryAnalyticsJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Achievements/badges for children. Awarded when story_completed with childId.
 */
@Service
class AchievementService(
    private val childAchievementJpaRepository: ChildAchievementJpaRepository,
    private val childJpaRepository: ChildJpaRepository,
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val definitions = listOf(
        AchievementDef("FIRST_STORY", "🌟 First Story", "Listen to your first story", 1),
        AchievementDef("STORY_EXPLORER_5", "📚 Story Explorer", "Complete 5 stories", 5),
        AchievementDef("STORY_CHAMPION_10", "🏆 Story Champion", "Complete 10 stories", 10),
        AchievementDef("STORY_MASTER_25", "⭐ Story Master", "Complete 25 stories", 25),
        AchievementDef("STORY_LEGEND_50", "👑 Story Legend", "Complete 50 stories", 50)
    )

    @Transactional
    fun checkAndAwardOnCompletion(childId: Long) {
        val child = childJpaRepository.findById(childId).orElse(null) ?: return
        val completions = storyAnalyticsJpaRepository.countByChild_IdAndEventType(childId, "story_completed")
        for (def in definitions) {
            if (completions >= def.requiredCompletions && !childAchievementJpaRepository.existsByChild_IdAndAchievementType(childId, def.type)) {
                childAchievementJpaRepository.save(
                    ChildAchievementEntity(
                        child = child,
                        achievementType = def.type
                    )
                )
                log.info("Awarded achievement {} to child {}", def.type, childId)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getByChildId(childId: Long): List<AchievementDto> {
        if (childJpaRepository.findById(childId).isEmpty) return emptyList()
        val earnedMap = childAchievementJpaRepository.findByChild_Id(childId).associate { it.achievementType to it.earnedAt }
        return definitions.map { def ->
            AchievementDto(
                type = def.type,
                name = def.name,
                description = def.description,
                requiredCompletions = def.requiredCompletions,
                earned = def.type in earnedMap,
                earnedAt = earnedMap[def.type]
            )
        }
    }

    fun getDefinitions(): List<AchievementDefinitionDto> =
        definitions.map { AchievementDefinitionDto(it.type, it.name, it.description, it.requiredCompletions) }

    private data class AchievementDef(
        val type: String,
        val name: String,
        val description: String,
        val requiredCompletions: Long
    )
}

data class AchievementDto(
    val type: String,
    val name: String,
    val description: String,
    val requiredCompletions: Long,
    val earned: Boolean,
    val earnedAt: java.time.Instant? = null
)

data class AchievementDefinitionDto(
    val type: String,
    val name: String,
    val description: String,
    val requiredCompletions: Long
)
