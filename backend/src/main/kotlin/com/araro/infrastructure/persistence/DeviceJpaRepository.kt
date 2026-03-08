package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceJpaRepository : JpaRepository<DeviceEntity, Long> {
    fun existsByParentIdAndDeviceHash(parentId: Long, deviceHash: String): Boolean
    fun findByParentId(parentId: Long): List<DeviceEntity>
    fun countByParentId(parentId: Long): Long
}
