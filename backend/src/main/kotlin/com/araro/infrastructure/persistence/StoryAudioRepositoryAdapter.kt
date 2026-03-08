package com.araro.infrastructure.persistence

import com.araro.application.port.StoryAudioRepositoryPort
import com.araro.domain.StoryAudio
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StoryAudioRepositoryAdapter(
    private val jpaRepository: StoryAudioJpaRepository
) : StoryAudioRepositoryPort {

    override fun save(audio: StoryAudio): StoryAudio {
        val entity = StoryAudioEntity(
            id = audio.id,
            masterStoryId = audio.masterStoryId,
            language = audio.language,
            audioFileUrl = audio.audioFileUrl,
            createdAt = audio.createdAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryAudio? =
        jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.toDomain()

    override fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean =
        jpaRepository.existsByMasterStoryIdAndLanguage(masterStoryId, language)

    override fun deleteByMasterStoryIdAndLanguage(masterStoryId: Long, language: String) {
        jpaRepository.deleteByMasterStoryIdAndLanguage(masterStoryId, language)
    }

    override fun deleteByMasterStoryId(masterStoryId: Long) {
        jpaRepository.deleteByMasterStoryId(masterStoryId)
    }

    override fun findAllLegacy(): List<StoryAudio> =
        jpaRepository.findAll().map { it.toDomain() }

    override fun deleteAllLegacy(): Int = jpaRepository.deleteAllLegacy()
}

private fun StoryAudioEntity.toDomain() = StoryAudio(
    id = id,
    masterStoryId = masterStoryId,
    language = language,
    audioFileUrl = audioFileUrl,
    createdAt = createdAt
)
