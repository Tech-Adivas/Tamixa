package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface MagicLinkTokenJpaRepository : JpaRepository<MagicLinkTokenEntity, Long> {
    fun findByTokenAndUsedAtIsNull(token: String): MagicLinkTokenEntity?
    fun findFirstByEmailAndShortCodeAndUsedAtIsNullOrderByCreatedAtDesc(
        email: String,
        shortCode: String
    ): MagicLinkTokenEntity?
}
