package com.araro.application.port

import com.araro.domain.Parent

interface ParentRepositoryPort {

    fun save(parent: Parent): Parent

    fun findById(id: Long): Parent?

    fun findByEmail(email: String): Parent?

    fun findByPhone(phone: String): Parent?

    fun existsByEmail(email: String): Boolean
}
