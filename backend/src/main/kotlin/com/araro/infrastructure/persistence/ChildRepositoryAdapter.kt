package com.araro.infrastructure.persistence

import com.araro.application.port.ChildRepositoryPort
import com.araro.domain.Child
import org.springframework.stereotype.Component

@Component
class ChildRepositoryAdapter(
    private val jpaRepository: ChildJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : ChildRepositoryPort {

    override fun save(child: Child): Child {
        val parentEntity = parentJpaRepository.findById(child.parentId).orElseThrow {
            IllegalArgumentException("Parent not found: ${child.parentId}")
        }
        val entity = ChildEntity(
            id = child.id.takeIf { it > 0 } ?: 0,
            parent = parentEntity,
            name = child.name,
            dateOfBirth = child.dateOfBirth,
            languagePreference = child.languagePreference,
            interests = child.interests,
            createdAt = child.createdAt,
            favoriteColor = child.favoriteColor,
            favoriteAnimal = child.favoriteAnimal,
            characterTraits = child.characterTraits,
            avatarChoice = child.avatarChoice
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): Child? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByParentId(parentId: Long): List<Child> {
        return jpaRepository.findByParent_Id(parentId).map { it.toDomain() }
    }

    override fun existsByIdAndParentId(id: Long, parentId: Long): Boolean {
        return jpaRepository.findByIdAndParent_Id(id, parentId) != null
    }
}

private fun ChildEntity.toDomain(): Child = Child(
    id = id,
    parentId = parent.id,
    name = name,
    dateOfBirth = dateOfBirth,
    languagePreference = languagePreference,
    interests = interests,
    createdAt = createdAt,
    favoriteColor = favoriteColor,
    favoriteAnimal = favoriteAnimal,
    characterTraits = characterTraits,
    avatarChoice = avatarChoice
)
