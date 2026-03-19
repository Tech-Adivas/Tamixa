package com.tamixa.infrastructure.persistence

import com.tamixa.domain.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ParentJpaRepository : JpaRepository<ParentEntity, Long> {

    fun findByEmail(email: String): ParentEntity?

    fun findByPhone(phone: String): ParentEntity?

    fun existsByEmail(email: String): Boolean

    fun existsByPhone(phone: String): Boolean

    fun findByRoleIn(roles: List<Role>, pageable: Pageable): Page<ParentEntity>

    fun findByEmailContainingIgnoreCase(email: String, pageable: Pageable): Page<ParentEntity>

    fun findBySuspendedAtIsNull(pageable: Pageable): Page<ParentEntity>

    fun findBySuspendedAtIsNotNull(pageable: Pageable): Page<ParentEntity>

    fun findByEmailContainingIgnoreCaseAndSuspendedAtIsNull(email: String, pageable: Pageable): Page<ParentEntity>

    fun findByEmailContainingIgnoreCaseAndSuspendedAtIsNotNull(email: String, pageable: Pageable): Page<ParentEntity>
}
