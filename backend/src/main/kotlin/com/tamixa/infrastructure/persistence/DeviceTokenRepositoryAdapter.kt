package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.DeviceToken
import com.tamixa.application.port.DeviceTokenRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeviceTokenRepositoryAdapter(
    private val jpaRepository: DeviceTokenJpaRepository
) : DeviceTokenRepositoryPort {

    @Transactional
    override fun save(deviceToken: DeviceToken): DeviceToken {
        val entity = DeviceTokenEntity(
            id = deviceToken.id,
            parentId = deviceToken.parentId,
            token = deviceToken.token,
            platform = deviceToken.platform,
            createdAt = deviceToken.createdAt,
            lastUsedAt = deviceToken.lastUsedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByParentId(parentId: Long): List<DeviceToken> {
        return jpaRepository.findByParentId(parentId).map { it.toDomain() }
    }

    override fun findByToken(token: String): DeviceToken? {
        return jpaRepository.findByToken(token)?.toDomain()
    }

    @Transactional
    override fun deleteByToken(token: String) {
        jpaRepository.deleteByToken(token)
    }

    @Transactional
    override fun deleteByParentId(parentId: Long) {
        jpaRepository.deleteByParentId(parentId)
    }

    private fun DeviceTokenEntity.toDomain() = DeviceToken(
        id = id,
        parentId = parentId,
        token = token,
        platform = platform,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}
