package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ParentAvatarJpaRepository : JpaRepository<ParentAvatarEntity, Long> {
    fun findByParentId(parentId: Long): ParentAvatarEntity?
    fun existsByParentId(parentId: Long): Boolean
    fun deleteByParentId(parentId: Long)
}
