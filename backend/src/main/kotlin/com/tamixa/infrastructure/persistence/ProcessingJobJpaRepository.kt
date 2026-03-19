package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ProcessingJobJpaRepository : JpaRepository<ProcessingJobEntity, Long> {

    fun findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
        resourceType: String,
        resourceId: String
    ): List<ProcessingJobEntity>

    fun findTop10ByOrderByCreatedAtDesc(): List<ProcessingJobEntity>
}
