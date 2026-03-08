package com.araro.infrastructure.persistence

import com.araro.application.port.ParentRepositoryPort
import com.araro.domain.Parent
import org.springframework.stereotype.Component

@Component
class ParentRepositoryAdapter(
    private val jpaRepository: ParentJpaRepository
) : ParentRepositoryPort {

    override fun save(parent: Parent): Parent {
        val entity = ParentEntity(
            id = parent.id.takeIf { it > 0 } ?: 0,
            email = parent.email,
            passwordHash = parent.passwordHash,
            role = parent.role,
            createdAt = parent.createdAt,
            phone = parent.phone,
            suspendedAt = parent.suspendedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
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
}

private fun ParentEntity.toDomain(): Parent = Parent(
    id = id,
    email = email,
    passwordHash = passwordHash,
    role = role,
    createdAt = createdAt,
    phone = phone,
    suspendedAt = suspendedAt
)
