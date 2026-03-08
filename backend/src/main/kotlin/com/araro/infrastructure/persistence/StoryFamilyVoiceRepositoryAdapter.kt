package com.araro.infrastructure.persistence

import com.araro.application.port.StoryFamilyVoiceRepositoryPort
import com.araro.domain.StoryFamilyVoice
import org.springframework.stereotype.Component

@Component
class StoryFamilyVoiceRepositoryAdapter(
    private val jpaRepository: StoryFamilyVoiceJpaRepository
) : StoryFamilyVoiceRepositoryPort {

    override fun save(voice: StoryFamilyVoice): StoryFamilyVoice =
        jpaRepository.save(StoryFamilyVoiceEntity.from(voice)).toDomain()

    override fun findByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): StoryFamilyVoice? =
        jpaRepository.findByStoryIdAndParentIdAndLanguage(storyId, parentId, language)?.toDomain()

    override fun findByStoryId(storyId: Long): List<StoryFamilyVoice> =
        jpaRepository.findByStoryId(storyId).map { it.toDomain() }

    override fun existsByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): Boolean =
        jpaRepository.existsByStoryIdAndParentIdAndLanguage(storyId, parentId, language)

    override fun deleteByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String) {
        jpaRepository.deleteByStoryIdAndParentIdAndLanguage(storyId, parentId, language)
    }

    override fun deleteByStoryId(storyId: Long) {
        jpaRepository.deleteByStoryId(storyId)
    }
}
