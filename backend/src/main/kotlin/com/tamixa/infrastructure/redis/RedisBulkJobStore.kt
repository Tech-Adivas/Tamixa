package com.tamixa.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tamixa.application.port.BulkJobStorePort
import com.tamixa.application.storylibrary.BulkJobState
import com.tamixa.application.storylibrary.BulkJobStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Redis-backed store for async bulk story generation jobs.
 * Supports multi-instance deployment with distributed state.
 * Jobs expire after 1 hour (TTL) to prevent unbounded memory growth.
 */
@Component
@ConditionalOnProperty("app.bulk-job.use-redis", havingValue = "true")
class RedisBulkJobStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : BulkJobStorePort {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val JOB_KEY_PREFIX = "bulk-job:"
        private const val RUNNING_JOB_KEY = "bulk-job:running"
        private const val COMPLETED_JOB_TTL_SECONDS = 3600L // 1 hour
    }

    override fun create(requestedTotal: Int, publish: Boolean): String {
        val jobId = UUID.randomUUID().toString()
        val state = BulkJobState(
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
        saveState(jobId, state)
        logger.debug("Created bulk job {} in Redis", jobId)
        return jobId
    }

    override fun get(jobId: String): BulkJobState? {
        val key = "$JOB_KEY_PREFIX$jobId"
        val json = redisTemplate.opsForValue().get(key) ?: return null
        return try {
            objectMapper.readValue<BulkJobState>(json)
        } catch (e: Exception) {
            logger.error("Failed to deserialize job state for {}: {}", jobId, e.message)
            null
        }
    }

    override fun tryAcquire(jobId: String): Boolean {
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(RUNNING_JOB_KEY, jobId, Duration.ofHours(1))
        if (acquired == true) {
            logger.debug("Acquired lock for bulk job {}", jobId)
        }
        return acquired == true
    }

    override fun release(jobId: String) {
        val current = redisTemplate.opsForValue().get(RUNNING_JOB_KEY)
        if (current == jobId) {
            redisTemplate.delete(RUNNING_JOB_KEY)
            logger.debug("Released lock for bulk job {}", jobId)
        }
    }

    override fun isBulkRunning(): Boolean {
        return redisTemplate.hasKey(RUNNING_JOB_KEY) == true
    }

    override fun getRunningJobId(): String? {
        return redisTemplate.opsForValue().get(RUNNING_JOB_KEY)
    }

    override fun updateProgress(
        jobId: String,
        currentIndex: Int,
        createdCount: Int,
        failedCount: Int,
        created: List<Map<String, Any?>>,
        failed: List<Map<String, Any?>>
    ) {
        val state = get(jobId) ?: return
        val updated = state.copy(
            currentIndex = currentIndex,
            createdCount = createdCount,
            failedCount = failedCount,
            created = created,
            failed = failed
        )
        saveState(jobId, updated)
    }

    override fun complete(jobId: String, result: Map<String, Any>) {
        val state = get(jobId) ?: return
        val updated = state.copy(
            status = BulkJobStatus.COMPLETED,
            currentIndex = state.requestedTotal,
            createdCount = (result["createdCount"] as? Number)?.toInt() ?: 0,
            failedCount = (result["failedCount"] as? Number)?.toInt() ?: 0,
            created = (result["created"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList(),
            failed = (result["failed"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList(),
            completedAt = Instant.now()
        )
        saveState(jobId, updated, COMPLETED_JOB_TTL_SECONDS)
        release(jobId)
        logger.info("Completed bulk job {} with {} created, {} failed", jobId, updated.createdCount, updated.failedCount)
    }

    override fun fail(jobId: String, message: String) {
        val state = get(jobId) ?: return
        val updated = state.copy(
            status = BulkJobStatus.FAILED,
            errorMessage = message,
            completedAt = Instant.now()
        )
        saveState(jobId, updated, COMPLETED_JOB_TTL_SECONDS)
        release(jobId)
        logger.error("Failed bulk job {}: {}", jobId, message)
    }

    override fun setRunning(jobId: String) {
        val state = get(jobId) ?: return
        val updated = state.copy(
            startedAt = Instant.now(),
            status = BulkJobStatus.RUNNING
        )
        saveState(jobId, updated)
        logger.info("Started bulk job {}", jobId)
    }

    private fun saveState(jobId: String, state: BulkJobState, ttlSeconds: Long? = null) {
        val key = "$JOB_KEY_PREFIX$jobId"
        val json = objectMapper.writeValueAsString(state)
        if (ttlSeconds != null) {
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds))
        } else {
            redisTemplate.opsForValue().set(key, json)
        }
    }

    // Note: Redis TTL handles expiration automatically; no scheduled eviction needed
}
