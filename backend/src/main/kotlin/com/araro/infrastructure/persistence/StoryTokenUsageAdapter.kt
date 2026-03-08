package com.araro.infrastructure.persistence

import com.araro.application.port.StoryTokenUsagePort
import org.springframework.stereotype.Component

@Component
class StoryTokenUsageAdapter(
    private val jpaRepository: StoryTokenUsageJpaRepository,
    private val storyJpaRepository: StoryJpaRepository
) : StoryTokenUsagePort {

    override fun recordUsage(storyId: Long, promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        val story = storyJpaRepository.findById(storyId).orElse(null) ?: return
        val entity = StoryTokenUsageEntity(
            story = story,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens
        )
        jpaRepository.save(entity)
    }
}
