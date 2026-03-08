package com.araro.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChildJpaRepository : JpaRepository<ChildEntity, Long> {

    fun findByParent_Id(parentId: Long): List<ChildEntity>

    fun findByParent_Id(parentId: Long, pageable: Pageable): Page<ChildEntity>

    fun findByIdAndParent_Id(id: Long, parentId: Long): ChildEntity?
}
