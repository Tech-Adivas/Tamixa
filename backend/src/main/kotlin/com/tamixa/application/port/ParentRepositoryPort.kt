package com.tamixa.application.port

import com.tamixa.domain.Parent

interface ParentRepositoryPort {

    fun save(parent: Parent): Parent

    fun findById(id: Long): Parent?

    fun findByEmail(email: String): Parent?

    fun findByPhone(phone: String): Parent?

    fun existsByEmail(email: String): Boolean

    /** True if any parent (any role) has this phone number. Used to avoid duplicate mobile numbers. */
    fun existsByPhone(phone: String): Boolean

    fun deleteById(id: Long): Boolean
}
