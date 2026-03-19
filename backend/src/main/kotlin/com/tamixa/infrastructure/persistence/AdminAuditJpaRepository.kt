package com.tamixa.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAuditJpaRepository : JpaRepository<AdminAuditEntity, Long> {

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AdminAuditEntity>
}
