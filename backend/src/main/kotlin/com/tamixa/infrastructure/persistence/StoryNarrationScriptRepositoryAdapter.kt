package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import com.tamixa.domain.narration.StoryNarrationScript
import com.tamixa.domain.narration.ToneMode
import org.springframework.stereotype.Component

@Component
class StoryNarrationScriptRepositoryAdapter(
    private val jpaRepository: StoryNarrationScriptJpaRepository
) : StoryNarrationScriptRepositoryPort {

    override fun save(script: StoryNarrationScript): StoryNarrationScript {
        val entity = StoryNarrationScriptEntity(
            id = script.id,
            translationId = script.translationId,
            toneMode = script.toneMode,
            scriptText = script.scriptText,
            safetyScore = script.safetyScore,
            createdAt = script.createdAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByTranslationId(translationId: Long): StoryNarrationScript? =
        jpaRepository.findByTranslationId(translationId)?.toDomain()
}

private fun StoryNarrationScriptEntity.toDomain() = StoryNarrationScript(
    id = id,
    translationId = translationId,
    toneMode = toneMode,
    scriptText = scriptText,
    safetyScore = safetyScore,
    createdAt = createdAt
)
