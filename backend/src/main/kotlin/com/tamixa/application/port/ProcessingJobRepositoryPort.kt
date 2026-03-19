package com.tamixa.application.port

import java.time.Instant

/**
 * Port for generic processing job persistence.
 * Used for AI workflow tracking (story polish, TTS, voice cloning, avatar video).
 */
interface ProcessingJobRepositoryPort {

    fun save(
        jobType: String,
        resourceType: String,
        resourceId: String,
        status: String,
        progress: Int = 0,
        startedAt: Instant? = null,
        finishedAt: Instant? = null,
        errorMessage: String? = null,
        metadata: String? = null
    ): Long

    fun findById(id: Long): ProcessingJobRecord?

    fun findByResource(resourceType: String, resourceId: String): List<ProcessingJobRecord>

    fun updateStatus(id: Long, status: String, progress: Int, finishedAt: Instant?, errorMessage: String?): Boolean

    fun findRecent(limit: Int): List<ProcessingJobRecord>
}

data class ProcessingJobRecord(
    val id: Long,
    val jobType: String,
    val resourceType: String,
    val resourceId: String,
    val status: String,
    val progress: Int,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val errorMessage: String?,
    val metadata: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
