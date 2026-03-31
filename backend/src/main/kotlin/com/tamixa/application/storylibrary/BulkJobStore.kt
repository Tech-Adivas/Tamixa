package com.tamixa.application.storylibrary

import com.tamixa.application.port.BulkJobStorePort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory store for async bulk story generation jobs.
 * Supports progress polling and one-bulk-at-a-time guard.
 * Completed/failed jobs are evicted after 1 hour to prevent memory leaks.
 * For multi-instance deployment, use RedisBulkJobStore (set app.bulk-job.use-redis=true).
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    "app.bulk-job.use-redis",
    havingValue = "false",
    matchIfMissing = true
)
class BulkJobStore : BulkJobStorePort {

    private val jobs = ConcurrentHashMap<String, BulkJobState>()
    private val runningJobId = AtomicReference<String?>(null)

    companion object {
        private const val COMPLETED_JOB_TTL_SECONDS = 3600L // 1 hour
    }

    override fun create(requestedTotal: Int, publish: Boolean): String {
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

    override fun get(jobId: String): BulkJobState? = jobs[jobId]

    override fun tryAcquire(jobId: String): Boolean {
        return runningJobId.compareAndSet(null, jobId)
    }

    override fun release(jobId: String) {
        runningJobId.compareAndSet(jobId, null)
    }

    override fun isBulkRunning(): Boolean = runningJobId.get() != null

    override fun getRunningJobId(): String? = runningJobId.get()

    override fun updateProgress(jobId: String, currentIndex: Int, createdCount: Int, failedCount: Int, created: List<Map<String, Any?>>, failed: List<Map<String, Any?>>) {
        jobs[jobId]?.let { state ->
            jobs[jobId] = state.copy(
                currentIndex = currentIndex,
                createdCount = createdCount,
                failedCount = failedCount,
                created = created.toList(),
                failed = failed.toList()
            )
        }
    }

    override fun complete(jobId: String, result: Map<String, Any>) {
        jobs[jobId]?.let { state ->
            jobs[jobId] = state.copy(
                status = BulkJobStatus.COMPLETED,
                currentIndex = state.requestedTotal,
                createdCount = (result["createdCount"] as? Number)?.toInt() ?: 0,
                failedCount = (result["failedCount"] as? Number)?.toInt() ?: 0,
                created = (result["created"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList(),
                failed = (result["failed"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList(),
                completedAt = Instant.now()
            )
        }
        release(jobId)
    }

    override fun fail(jobId: String, message: String) {
        jobs[jobId]?.let { state ->
            jobs[jobId] = state.copy(
                status = BulkJobStatus.FAILED,
                errorMessage = message,
                completedAt = Instant.now()
            )
        }
        release(jobId)
    }

    override fun setRunning(jobId: String) {
        jobs[jobId]?.let { state ->
            jobs[jobId] = state.copy(
                startedAt = Instant.now(),
                status = BulkJobStatus.RUNNING
            )
        }
    }

    /** Evict completed/failed jobs older than 1 hour to prevent unbounded memory growth. */
    @Scheduled(fixedDelay = 600_000) // every 10 minutes
    fun evictExpiredJobs() {
        val cutoff = Instant.now().minusSeconds(COMPLETED_JOB_TTL_SECONDS)
        jobs.entries.removeIf { (_, state) ->
            val terminal = state.status == BulkJobStatus.COMPLETED || state.status == BulkJobStatus.FAILED
            terminal && state.completedAt != null && state.completedAt!!.isBefore(cutoff)
        }
    }
}
