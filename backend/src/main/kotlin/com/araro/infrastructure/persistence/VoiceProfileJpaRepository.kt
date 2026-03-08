package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface VoiceProfileJpaRepository : JpaRepository<VoiceProfileEntity, Long> {

    fun findByParent_Id(parentId: Long): List<VoiceProfileEntity>

    fun findByIdAndParent_Id(id: Long, parentId: Long): VoiceProfileEntity?
}
