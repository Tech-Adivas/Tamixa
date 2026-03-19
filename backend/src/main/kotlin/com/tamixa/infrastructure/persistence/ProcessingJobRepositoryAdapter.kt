package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ProcessingJobRecord
import com.tamixa.application.port.ProcessingJobRepositoryPort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProcessingJobRepositoryAdapter(
    private val jpaRepository: ProcessingJobJpaRepository
) : ProcessingJobRepositoryPort {

    override fun save(
        jobType: String,
        resourceType: String,
        resourceId: String,
        status: String,
        progress: Int,
        startedAt: Instant?,
        finishedAt: Instant?,
        errorMessage: String?,
        metadata: String?
    ): Long {
        val now = Instant.now()
        val entity = ProcessingJobEntity(
            jobType = jobType,
            resourceType = resourceType,
            resourceId = resourceId,
            status = status,
            progress = progress,
            startedAt = startedAt,
            finishedAt = finishedAt,
            errorMessage = errorMessage,
            metadata = metadata,
            updatedAt = now
        )
        return jpaRepository.save(entity).id
    }

    override fun findById(id: Long): ProcessingJobRecord? =
        jpaRepository.findById(id).orElse(null)?.toRecord()

    override fun findByResource(resourceType: String, resourceId: String): List<ProcessingJobRecord> =
        jpaRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId)
            .map { it.toRecord() }

    override fun updateStatus(
        id: Long,
        status: String,
        progress: Int,
        finishedAt: Instant?,
        errorMessage: String?
    ): Boolean {
        val entity = jpaRepository.findById(id).orElse(null) ?: return false
        entity.status = status
        entity.progress = progress
        entity.finishedAt = finishedAt
        entity.errorMessage = errorMessage
        entity.updatedAt = Instant.now()
        jpaRepository.save(entity)
        return true
    }

    override fun findRecent(limit: Int): List<ProcessingJobRecord> =
        jpaRepository.findTop10ByOrderByCreatedAtDesc().take(limit).map { it.toRecord() }
}

private fun ProcessingJobEntity.toRecord() = ProcessingJobRecord(
    id = id,
    jobType = jobType,
    resourceType = resourceType,
    resourceId = resourceId,
    status = status,
    progress = progress,
    startedAt = startedAt,
    finishedAt = finishedAt,
    errorMessage = errorMessage,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt
)
