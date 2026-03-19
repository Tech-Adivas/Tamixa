package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.domain.Parent
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
            nickname = parent.nickname,
            displayName = parent.displayName,
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
    suspendedAt = suspendedAt
)
