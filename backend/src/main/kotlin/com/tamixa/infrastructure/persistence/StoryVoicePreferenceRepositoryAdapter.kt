package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.StoryVoicePreferenceRepositoryPort
import com.tamixa.domain.StoryVoicePreference
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StoryVoicePreferenceRepositoryAdapter(
    private val jpaRepository: StoryVoicePreferenceJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : StoryVoicePreferenceRepositoryPort {

    override fun find(parentId: Long, storyId: Long, storySource: String): StoryVoicePreference? =
        jpaRepository.findByParent_IdAndStoryIdAndStorySource(parentId, storyId, storySource)?.toDomain()

    override fun save(preference: StoryVoicePreference): StoryVoicePreference {
        val parent = parentJpaRepository.findById(preference.parentId).orElseThrow { IllegalArgumentException("Parent not found: ${preference.parentId}") }
        val existing = jpaRepository.findByParent_IdAndStoryIdAndStorySource(
            preference.parentId,
            preference.storyId,
            preference.storySource
        )
        val now = Instant.now()
        val entity = if (existing != null) {
            existing.voiceProfile = preference.voiceProfile
            existing.playbackMode = preference.playbackMode.trim().take(20).ifEmpty { "default" }
            existing.updatedAt = now
            existing
        } else {
            StoryVoicePreferenceEntity(
                id = 0,
                parent = parent,
                storyId = preference.storyId,
                storySource = preference.storySource,
                voiceProfile = preference.voiceProfile,
                playbackMode = preference.playbackMode.trim().take(20).ifEmpty { "default" },
                createdAt = now,
                updatedAt = now
            )
        }
        return jpaRepository.save(entity).toDomain()
    }

    override fun delete(parentId: Long, storyId: Long, storySource: String) {
        jpaRepository.deleteByParent_IdAndStoryIdAndStorySource(parentId, storyId, storySource)
    }
}

private fun StoryVoicePreferenceEntity.toDomain() = StoryVoicePreference(
    id = id,
    parentId = parent.id,
    storyId = storyId,
    storySource = storySource,
    voiceProfile = voiceProfile,
    playbackMode = playbackMode,
    createdAt = createdAt,
    updatedAt = updatedAt
)
