package com.araro.application.narration

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
    @Value("\${app.narration.max-concurrent-rewrite:4}") private val maxConcurrent: Int
) {
    private val semaphore = Semaphore(maxConcurrent.coerceIn(1, 8), true)
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> withPermit(timeoutMs: Long = 120_000, block: () -> T): T {
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            log.warn("Rewrite concurrency limit reached, permit not acquired within {}ms", timeoutMs)
            throw IllegalStateException("Rewrite permit not acquired—too many concurrent rewrite calls")
        }
        return try {
            block()
        } finally {
            semaphore.release()
        }
    }
}
