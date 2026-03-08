package com.araro.infrastructure.persistence

import com.araro.application.port.DevicePort
import org.springframework.stereotype.Component

@Component
class DeviceRepositoryAdapter(
    private val deviceJpaRepository: DeviceJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : DevicePort {

    override fun registerDevice(parentId: Long, deviceHash: String) {
        val parent = parentJpaRepository.findById(parentId).orElse(null) ?: return
        if (!deviceJpaRepository.existsByParentIdAndDeviceHash(parentId, deviceHash)) {
            deviceJpaRepository.save(
                DeviceEntity(
                    parent = parent,
                    deviceHash = deviceHash
                )
            )
        }
    }

    override fun hasDevice(parentId: Long, deviceHash: String): Boolean =
        deviceJpaRepository.existsByParentIdAndDeviceHash(parentId, deviceHash)

    override fun getDeviceCount(parentId: Long): Long =
        deviceJpaRepository.countByParentId(parentId)
}
