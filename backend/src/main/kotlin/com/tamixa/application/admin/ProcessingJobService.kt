package com.tamixa.application.admin

import com.tamixa.application.port.ProcessingJobRecord
import com.tamixa.application.port.ProcessingJobRepositoryPort
import org.springframework.stereotype.Service

@Service
class ProcessingJobService(
    private val repository: ProcessingJobRepositoryPort
) {

    fun getById(id: Long): ProcessingJobRecord? = repository.findById(id)

    fun getByResource(resourceType: String, resourceId: String): List<ProcessingJobRecord> =
        repository.findByResource(resourceType, resourceId)

    fun getRecent(limit: Int = 10): List<ProcessingJobRecord> =
        repository.findRecent(limit.coerceIn(1, 100))
}
