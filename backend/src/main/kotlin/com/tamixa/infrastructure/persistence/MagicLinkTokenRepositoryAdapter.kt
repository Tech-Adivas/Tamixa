package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.MagicLinkToken
import com.tamixa.application.port.MagicLinkTokenRepositoryPort
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

    override fun findValidByLoginToken(loginToken: String): MagicLinkToken? {
        val normalized = loginToken.trim().lowercase()
        val entity = jpaRepository.findByTokenAndUsedAtIsNull(normalized) ?: return null
        if (entity.expiresAt.isBefore(Instant.now())) return null
        val sc = entity.shortCode ?: return null
        return MagicLinkToken(
            id = entity.id,
            email = entity.email,
            token = entity.token,
            expiresAt = entity.expiresAt,
            shortCode = sc
        )
    }

    override fun markUsed(id: Long) {
        jpaRepository.findById(id).ifPresent { entity ->
            entity.usedAt = Instant.now()
            jpaRepository.save(entity)
        }
    }
}