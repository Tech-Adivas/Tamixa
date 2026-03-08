package com.araro.infrastructure.persistence

import com.araro.application.port.MagicLinkToken
import com.araro.application.port.MagicLinkTokenRepositoryPort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MagicLinkTokenRepositoryAdapter(
    private val jpaRepository: MagicLinkTokenJpaRepository
) : MagicLinkTokenRepositoryPort {

    override fun save(email: String, token: String, expiresAt: Instant, shortCode: String): Long {
        val entity = MagicLinkTokenEntity(
            email = email,
            token = token,
            expiresAt = expiresAt,
            shortCode = shortCode
        )
        return jpaRepository.save(entity).id
    }

    override fun findValidByEmailAndCode(email: String, shortCode: String): MagicLinkToken? {
        val entity = jpaRepository.findFirstByEmailAndShortCodeAndUsedAtIsNullOrderByCreatedAtDesc(
            email = email,
            shortCode = shortCode
        ) ?: return null
        if (entity.expiresAt.isBefore(Instant.now())) return null
        return MagicLinkToken(
            id = entity.id,
            email = entity.email,
            token = entity.token,
            expiresAt = entity.expiresAt,
            shortCode = entity.shortCode!!
        )
    }

    override fun markUsed(id: Long) {
        jpaRepository.findById(id).ifPresent { entity ->
            entity.usedAt = Instant.now()
            jpaRepository.save(entity)
        }
    }
}