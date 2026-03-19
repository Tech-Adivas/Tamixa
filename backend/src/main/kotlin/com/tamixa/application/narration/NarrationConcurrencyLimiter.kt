package com.tamixa.application.narration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Limits max concurrent narration jobs for load control.
 * Semaphore-based; blocks when limit reached (Kafka consumer can use short timeout).
 * Ensures system stays within capacity for 100 concurrent story jobs.
 *
 * Architecture: Why concurrency limiter? TTS providers (Azure, Google) enforce
 * rate limits. Unbounded jobs would breach quotas, exhaust threads, and cause
 * cascading failures. The limiter keeps throughput steady at scale (100k stories).
 */
@Component
class NarrationConcurrencyLimiter(
    @Value("\${app.narration.max-concurrent-jobs:100}") val maxConcurrent: Int
) {
    private val semaphore = Semaphore(maxConcurrent, true)
    private val log = LoggerFactory.getLogger(javaClass)

    fun tryAcquire(timeoutMs: Long = 5000): Boolean {
        return try {
            semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("Narration concurrency limiter interrupted")
            false
        }
    }

    fun release() = semaphore.release()

    fun availablePermits(): Int = semaphore.availablePermits()

    /** Current in-use count for tts_concurrency_current metric. */
    fun currentInUse(): Int = (maxConcurrent - semaphore.availablePermits()).coerceAtLeast(0)
}
