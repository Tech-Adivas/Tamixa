package com.araro.infrastructure.persistence

import com.araro.application.port.SoundscapeRepositoryPort
import com.araro.domain.Soundscape
import com.araro.domain.SoundscapeCategory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SoundscapeRepositoryAdapter(
    private val soundscapeJpaRepository: SoundscapeJpaRepository
) : SoundscapeRepositoryPort {

    @Transactional
    override fun save(soundscape: Soundscape): Soundscape {
        val entity = SoundscapeEntity(
            id = soundscape.id,
            name = soundscape.name,
            category = soundscape.category,
            durationSeconds = soundscape.durationSeconds,
            audioUrl = soundscape.audioUrl,
            description = soundscape.description,
            createdAt = soundscape.createdAt
        )
        val saved = soundscapeJpaRepository.save(entity)
        return Soundscape(
            id = saved.id,
            name = saved.name,
            category = saved.category,
            durationSeconds = saved.durationSeconds,
            audioUrl = saved.audioUrl,
            description = saved.description,
            createdAt = saved.createdAt
        )
    }

    override fun findById(id: Long): Soundscape? {
        return soundscapeJpaRepository.findById(id).map {
            Soundscape(
                id = it.id,
                name = it.name,
                category = it.category,
                durationSeconds = it.durationSeconds,
                audioUrl = it.audioUrl,
                description = it.description,
                createdAt = it.createdAt
            )
        }.orElse(null)
    }

    override fun findAll(): List<Soundscape> {
        return soundscapeJpaRepository.findAll().map {
            Soundscape(
                id = it.id,
                name = it.name,
                category = it.category,
                durationSeconds = it.durationSeconds,
                audioUrl = it.audioUrl,
                description = it.description,
                createdAt = it.createdAt
            )
        }
    }

    override fun getSoundscapesByCategory(category: SoundscapeCategory): List<Soundscape> {
        return soundscapeJpaRepository.findByCategory(category).map {
            Soundscape(
                id = it.id,
                name = it.name,
                category = it.category,
                durationSeconds = it.durationSeconds,
                audioUrl = it.audioUrl,
                description = it.description,
                createdAt = it.createdAt
            )
        }
    }

    override fun searchByName(name: String): List<Soundscape> {
        return soundscapeJpaRepository.findByNameContaining(name).map {
            Soundscape(
                id = it.id,
                name = it.name,
                category = it.category,
                durationSeconds = it.durationSeconds,
                audioUrl = it.audioUrl,
                description = it.description,
                createdAt = it.createdAt
            )
        }
    }
}
