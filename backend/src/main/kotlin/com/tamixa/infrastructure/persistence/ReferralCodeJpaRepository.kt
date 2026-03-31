package com.tamixa.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReferralCodeJpaRepository : JpaRepository<ReferralCodeEntity, Long> {
    fun findByShortcodeIgnoreCase(shortcode: String): ReferralCodeEntity?
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<ReferralCodeEntity>
}
