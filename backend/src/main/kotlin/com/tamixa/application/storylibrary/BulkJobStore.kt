package com.tamixa.application.storylibrary

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory store for async bulk story generation jobs.
 * Supports progress polling and one-bulk-at-a-time guard.
 * For multi-instance deployment, replace with Redis or DB-backed store.
 */
@Component
class BulkJobStore {

    private val jobs = ConcurrentHashMap<String, BulkJobState>()
    private val runningJobId = AtomicReference<String?>(null)

    fun create(requestedTotal: Int, publish: Boolean = false): String {
        val jobId = UUID.randomUUID().toString()
        jobs[jobId] = BulkJobState(
            jobId = jobId,
            status = BulkJobStatus.PENDING,
            requestedTotal = requestedTotal,
            publish = publish,
            currentIndex = 0,
            createdCount = 0,
            failedCount = 0,
            created = emptyList(),
            failed = emptyList(),
            errorMessage = null,
            startedAt = null,
            completedAt = null
        )
        return jobId
    }

    fun get(jobId: String): BulkJobState? = jobs[jobId]

    fun tryAcquire(jobId: String): Boolean {
        return runningJobId.compareAndSet(null, jobId)
    }

    fun release(jobId: String) {
        runningJobId.compareAndSet(jobId, null)
    }

    fun isBulkRunning(): Boolean = runningJobId.get() != null

    fun getRunningJobId(): String? = runningJobId.get()

    fun updateProgress(jobId: String, currentIndex: Int, createdCount: Int, failedCount: Int, created: List<Map<String, Any?>>, failed: List<Map<String, Any?>>) {
        jobs[jobId]?.let { state ->
            state.currentIndex = currentIndex
            state.createdCount = createdCount
            state.failedCount = failedCount
            state.created = created.toList()
            state.failed = failed.toList()
        }
    }

    fun complete(jobId: String, result: Map<String, Any>) {
        jobs[jobId]?.let { state ->
            state.status = BulkJobStatus.COMPLETED
            state.currentIndex = state.requestedTotal
            state.createdCount = (result["createdCount"] as? Number)?.toInt() ?: 0
            state.failedCount = (result["failedCount"] as? Number)?.toInt() ?: 0
            state.created = (result["created"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
            state.failed = (result["failed"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
            state.completedAt = Instant.now()
        }
        release(jobId)
    }

    fun fail(jobId: String, message: String) {
        jobs[jobId]?.let { state ->
            state.status = BulkJobStatus.FAILED
            state.errorMessage = message
            state.completedAt = Instant.now()
        }
        release(jobId)
    }

    fun setRunning(jobId: String) {
        jobs[jobId]?.let { it.startedAt = Instant.now(); it.status = BulkJobStatus.RUNNING }
    }
}
