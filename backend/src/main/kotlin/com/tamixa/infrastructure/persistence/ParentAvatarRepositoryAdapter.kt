package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ParentAvatarRepositoryPort
import com.tamixa.domain.ParentAvatar
import org.springframework.stereotype.Component

@Component
class ParentAvatarRepositoryAdapter(
    private val jpaRepository: ParentAvatarJpaRepository
) : ParentAvatarRepositoryPort {

    override fun findByParentId(parentId: Long): ParentAvatar? =
        jpaRepository.findByParentId(parentId)?.toDomain()

    override fun save(avatar: ParentAvatar): ParentAvatar {
        val entity = ParentAvatarEntity(
            id = avatar.id,
            parentId = avatar.parentId,
            storagePath = avatar.storagePath,
            contentType = avatar.contentType,
            fileSizeBytes = avatar.fileSizeBytes,
            createdAt = avatar.createdAt,
            updatedAt = avatar.updatedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun deleteByParentId(parentId: Long) {
        jpaRepository.deleteByParentId(parentId)
    }

    override fun existsByParentId(parentId: Long): Boolean =
        jpaRepository.existsByParentId(parentId)
}
