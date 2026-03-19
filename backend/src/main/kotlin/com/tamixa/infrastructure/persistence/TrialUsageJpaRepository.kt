package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface TrialUsageJpaRepository : JpaRepository<TrialUsageEntity, Long> {
    fun existsByDeviceHashAndEmailHash(deviceHash: String, emailHash: String): Boolean
    fun existsByParentId(parentId: Long): Boolean
    fun deleteByParent_Id(parentId: Long)
}
