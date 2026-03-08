package com.araro.application.narration.impl

import com.araro.application.narration.TTSService
import com.araro.application.narration.TtsConcurrencyLimiter
import com.araro.application.narration.VoiceSynthesisStrategy
import com.araro.infrastructure.observability.NarrationMetrics
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * TTS orchestration: bounded executor, circuit breaker, timeout, retries.
 * Load readiness: circuit breaker fails fast when TTS downstream is degraded.
 */
@Service
class TTSServiceImpl(
    private val voiceStrategy: VoiceSynthesisStrategy,
    private val narrationMetrics: NarrationMetrics,
    private val ttsConcurrencyLimiter: TtsConcurrencyLimiter,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    @Qualifier("narrationTtsExecutor") private val executor: ExecutorService,
    @Value("\${app.narration.tts-timeout-seconds:10}") private val timeoutSeconds: Int,
    @Value("\${app.narration.tts-max-retries:2}") private val maxRetries: Int
) : TTSService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker: CircuitBreaker = circuitBreakerRegistry.circuitBreaker("tts")

    override fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray? {
        return ttsConcurrencyLimiter.withPermit({
            var lastEx: Exception? = null
            repeat(maxRetries + 1) { attempt ->
                try {
                    val future = executor.submit(Callable {
                        narrationMetrics.recordTtsLatency {
                            voiceStrategy.synthesize(ssml, language, voiceProfile)
                        }
                    })
                    // CircuitBreaker expects non-null; wrap so null from TTS becomes exception
                    val result = circuitBreaker.executeCallable {
                        val raw = future.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                        raw ?: throw IllegalStateException("TTS synthesis returned null")
                    }
                    if (result.isNotEmpty()) {
                        narrationMetrics.recordTtsCharacters(ssml.length)
                        return@withPermit result
                    }
                } catch (e: Exception) {
                    lastEx = e
                    log.warn("TTS attempt {} failed: {}", attempt + 1, e.message)
                    narrationMetrics.recordTtsFailure()
                }
            }
            val errMsg = lastEx?.message ?: lastEx?.cause?.message ?: "TTS failed after ${maxRetries + 1} attempts"
            log.error("TTS failed after {} attempts: {}", maxRetries + 1, errMsg, lastEx)
            throw IllegalStateException(errMsg, lastEx)
        }) ?: null
    }
}
