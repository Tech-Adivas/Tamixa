package com.tamixa.application.narration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Limits concurrent TTS API calls to protect provider rate limits.
 * Separate from NarrationConcurrencyLimiter (job-level); this gates actual TTS synthesize() calls.
 */
@Component
class TtsConcurrencyLimiter(
    @Value("\${app.narration.max-concurrent-tts:8}") private val maxConcurrent: Int,
    @Value("\${app.translation-pipeline.tts-timeout-ms:90000}") private val defaultPermitTimeoutMs: Long
) {
    private val semaphore = Semaphore(maxConcurrent, true)
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> withPermit(block: () -> T, timeoutMs: Long = defaultPermitTimeoutMs): T? {
        return try {
            if (semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                try {
                    block()
                } finally {
                    semaphore.release()
                }
            } else {
                log.warn("TTS concurrency limit reached, permit not acquired within {}ms (availablePermits={})", timeoutMs, semaphore.availablePermits())
                null
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("TTS concurrency limiter interrupted")
            null
        }
    }
}
