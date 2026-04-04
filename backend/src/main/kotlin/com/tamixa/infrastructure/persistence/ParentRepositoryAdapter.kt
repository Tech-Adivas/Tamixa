package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.domain.Parent
import org.springframework.stereotype.Component

@Component
class ParentRepositoryAdapter(
    private val jpaRepository: ParentJpaRepository
) : ParentRepositoryPort {

    override fun save(parent: Parent): Parent {
        if (parent.id <= 0) {
            val entity = ParentEntity(
                id = 0,
                email = parent.email,
                passwordHash = parent.passwordHash,
                role = parent.role,
                createdAt = parent.createdAt,
                phone = parent.phone,
                nickname = parent.nickname,
                displayName = parent.displayName,
                suspendedAt = parent.suspendedAt,
                storyArtPersonalizationOptIn = parent.storyArtPersonalizationOptIn
            )
            return jpaRepository.save(entity).toDomain()
        }
        val existing = jpaRepository.findById(parent.id).orElseThrow {
            IllegalStateException("Parent not found for save: id=${parent.id}")
        }
        existing.email = parent.email
        existing.role = parent.role
        existing.phone = parent.phone
        existing.nickname = parent.nickname
        existing.displayName = parent.displayName
        existing.suspendedAt = parent.suspendedAt
        existing.storyArtPersonalizationOptIn = parent.storyArtPersonalizationOptIn
        return jpaRepository.save(existing).toDomain()
    }

    override fun findById(id: Long): Parent? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): Parent? {
        return jpaRepository.findByEmail(email)?.toDomain()
    }

    override fun findByPhone(phone: String): Parent? {
        return jpaRepository.findByPhone(phone)?.toDomain()
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.existsByEmail(email)
    }

    override fun existsByPhone(phone: String): Boolean {
        return jpaRepository.existsByPhone(phone)
    }

    override fun deleteById(id: Long): Boolean {
        return if (!jpaRepository.existsById(id)) false
        else {
            jpaRepository.deleteById(id)
            true
        }
    }
}

private fun ParentEntity.toDomain(): Parent = Parent(
    id = id,
    email = email,
    passwordHash = passwordHash,
    role = role,
    createdAt = createdAt,
    phone = phone,
    nickname = nickname,
    displayName = displayName,
    suspendedAt = suspendedAt,
    storyArtPersonalizationOptIn = storyArtPersonalizationOptIn
)
