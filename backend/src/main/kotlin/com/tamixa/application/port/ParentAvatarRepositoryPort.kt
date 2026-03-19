package com.tamixa.application.port

import com.tamixa.domain.ParentAvatar

interface ParentAvatarRepositoryPort {
    fun findByParentId(parentId: Long): ParentAvatar?
    fun save(avatar: ParentAvatar): ParentAvatar
    fun deleteByParentId(parentId: Long)
    fun existsByParentId(parentId: Long): Boolean
}
