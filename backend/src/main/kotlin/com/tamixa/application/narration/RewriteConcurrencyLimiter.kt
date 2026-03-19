package com.tamixa.application.narration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Limits concurrent rewrite (OpenAI) calls to avoid rate limits.
 * With parallel language processing, 6+ simultaneous rewrites can trigger 429.
 */
@Component
class RewriteConcurrencyLimiter(
    @Value("\${app.narration.max-concurrent-rewrite:5}") private val maxConcurrent: Int
) {
    private val semaphore = Semaphore(maxConcurrent.coerceIn(1, 8), true)
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("RewriteConcurrencyLimiter: maxConcurrent={} (must be >= translation-pipeline.parallelism)", maxConcurrent)
    }

    fun <T> withPermit(timeoutMs: Long = 120_000, block: () -> T): T {
        val waitStart = System.currentTimeMillis()
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            log.warn("Rewrite concurrency limit reached, permit not acquired within {}ms (availablePermits={})", timeoutMs, semaphore.availablePermits())
            throw IllegalStateException("Rewrite permit not acquired—too many concurrent rewrite calls")
        }
        val waitMs = System.currentTimeMillis() - waitStart
        if (waitMs > 5000) {
            log.info("Rewrite permit acquired after {}ms wait (availablePermits={})", waitMs, semaphore.availablePermits())
        }
        return try {
            block()
        } finally {
            semaphore.release()
        }
    }
}
