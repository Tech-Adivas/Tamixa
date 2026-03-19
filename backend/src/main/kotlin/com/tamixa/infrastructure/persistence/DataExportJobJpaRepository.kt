package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface DataExportJobJpaRepository : JpaRepository<DataExportJobEntity, Long> {
    fun findByParent_IdOrderByRequestedAtDesc(parentId: Long, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<DataExportJobEntity>
    fun findByStatus(status: String, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<DataExportJobEntity>
    fun deleteByParent_Id(parentId: Long)
}
