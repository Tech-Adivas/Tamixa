package com.araro.infrastructure.persistence

import com.araro.application.port.SoundscapeUsageRepositoryPort
import com.araro.domain.SoundscapeUsage
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class SoundscapeUsageRepositoryAdapter(
    private val soundscapeUsageJpaRepository: SoundscapeUsageJpaRepository
) : SoundscapeUsageRepositoryPort {

    @Transactional
    override fun save(usage: SoundscapeUsage): SoundscapeUsage {
        val entity = SoundscapeUsageEntity(
            id = usage.id,
            parentId = usage.parentId,
            storyId = usage.storyId,
            soundscapeId = usage.soundscapeId,
            usageCount = usage.usageCount,
            lastUsedAt = usage.lastUsedAt,
            createdAt = usage.createdAt,
            updatedAt = usage.updatedAt
        )
        val saved = soundscapeUsageJpaRepository.save(entity)
        return SoundscapeUsage(
            id = saved.id,
            parentId = saved.parentId,
            storyId = saved.storyId,
            soundscapeId = saved.soundscapeId,
            usageCount = saved.usageCount,
            lastUsedAt = saved.lastUsedAt,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt
        )
    }

    override fun findByParentId(parentId: Long): List<SoundscapeUsage> {
        return soundscapeUsageJpaRepository.findByParentId(parentId).map {
            SoundscapeUsage(
                id = it.id,
                parentId = it.parentId,
                storyId = it.storyId,
                soundscapeId = it.soundscapeId,
                usageCount = it.usageCount,
                lastUsedAt = it.lastUsedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    override fun findByStoryId(storyId: Long): List<SoundscapeUsage> {
        return soundscapeUsageJpaRepository.findByStoryId(storyId).map {
            SoundscapeUsage(
                id = it.id,
                parentId = it.parentId,
                storyId = it.storyId,
                soundscapeId = it.soundscapeId,
                usageCount = it.usageCount,
                lastUsedAt = it.lastUsedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    override fun findByParentIdAndSoundscapeId(parentId: Long, soundscapeId: Long): SoundscapeUsage? {
        return soundscapeUsageJpaRepository.findByParentIdAndSoundscapeId(parentId, soundscapeId)?.let {
            SoundscapeUsage(
                id = it.id,
                parentId = it.parentId,
                storyId = it.storyId,
                soundscapeId = it.soundscapeId,
                usageCount = it.usageCount,
                lastUsedAt = it.lastUsedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    @Transactional
    override fun incrementUsage(parentId: Long, storyId: Long?, soundscapeId: Long): SoundscapeUsage {
        soundscapeUsageJpaRepository.incrementUsage(parentId, storyId, soundscapeId, Instant.now())
        val entity = soundscapeUsageJpaRepository.findByParentIdAndSoundscapeId(parentId, soundscapeId)
            ?: throw IllegalStateException("incrementUsage did not create/find soundscape usage parentId=$parentId soundscapeId=$soundscapeId")
        return SoundscapeUsage(
            id = entity.id,
            parentId = entity.parentId,
            storyId = entity.storyId,
            soundscapeId = entity.soundscapeId,
            usageCount = entity.usageCount,
            lastUsedAt = entity.lastUsedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
