package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ParentConsentJpaRepository : JpaRepository<ParentConsentEntity, Long> {
    fun findByParent_IdOrderByGrantedAtDesc(parentId: Long, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<ParentConsentEntity>
    fun deleteByParent_Id(parentId: Long)
}
