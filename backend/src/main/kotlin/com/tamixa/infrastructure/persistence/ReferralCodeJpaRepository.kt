package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ReferralCodeJpaRepository : JpaRepository<ReferralCodeEntity, Long> {
    fun findByShortcodeIgnoreCase(shortcode: String): ReferralCodeEntity?
    fun findAllByOrderByCreatedAtDesc(): List<ReferralCodeEntity>
}
