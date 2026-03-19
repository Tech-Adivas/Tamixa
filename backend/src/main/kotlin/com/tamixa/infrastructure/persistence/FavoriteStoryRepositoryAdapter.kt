package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.FavoriteStoryRepositoryPort
import com.tamixa.domain.FavoriteStory
import org.springframework.stereotype.Component

@Component
class FavoriteStoryRepositoryAdapter(
    private val jpaRepository: FavoriteStoryJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : FavoriteStoryRepositoryPort {

    override fun findByParentId(parentId: Long): List<FavoriteStory> =
        jpaRepository.findByParent_Id(parentId).map { it.toDomain() }

    override fun add(parentId: Long, storyId: Long, storySource: String): FavoriteStory {
        val parent = parentJpaRepository.findById(parentId).orElseThrow { IllegalArgumentException("Parent not found") }
        val existing = jpaRepository.findByParent_IdAndStoryId(parentId, storyId)
        if (existing != null) return existing.toDomain()
        val entity = FavoriteStoryEntity(
            parent = parent,
            storyId = storyId,
            storySource = storySource
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun remove(parentId: Long, storyId: Long) {
        jpaRepository.deleteByParent_IdAndStoryId(parentId, storyId)
    }

    override fun isFavorite(parentId: Long, storyId: Long): Boolean =
        jpaRepository.existsByParent_IdAndStoryId(parentId, storyId)
}

private fun FavoriteStoryEntity.toDomain() = FavoriteStory(
    id = id,
    parentId = parent.id,
    storyId = storyId,
    storySource = storySource,
    childId = child?.id,
    createdAt = createdAt
)
