package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface MagicLinkTokenJpaRepository : JpaRepository<MagicLinkTokenEntity, Long> {
    fun findByTokenAndUsedAtIsNull(token: String): MagicLinkTokenEntity?
    fun findFirstByEmailAndShortCodeAndUsedAtIsNullOrderByCreatedAtDesc(
        email: String,
        shortCode: String
    ): MagicLinkTokenEntity?
    fun deleteByEmail(email: String)
}
