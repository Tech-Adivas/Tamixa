package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceTokenJpaRepository : JpaRepository<DeviceTokenEntity, Long> {
    fun findByParentId(parentId: Long): List<DeviceTokenEntity>
    fun findByToken(token: String): DeviceTokenEntity?
    fun deleteByToken(token: String)
    fun deleteByParentId(parentId: Long)
}
