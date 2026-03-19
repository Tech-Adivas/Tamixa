package com.tamixa.application.narration.impl

import com.tamixa.application.narration.TTSService
import com.tamixa.application.narration.TtsConcurrencyLimiter
import com.tamixa.application.narration.VoiceSynthesisStrategy
import com.tamixa.infrastructure.observability.NarrationMetrics
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
            val isClonedVoice = voiceProfile.startsWith("cloned:")
            // Cloned voice can return null for expected reasons (e.g. profile has no Google key).
            // Do not run through circuit breaker so those nulls do not open the circuit.
            val result = if (isClonedVoice) {
                runClonedVoiceSynthesis(ssml, language, voiceProfile)
            } else {
                runWithCircuitBreakerAndRetries(ssml, language, voiceProfile)
            }
            result
        }) ?: null
    }

    private fun runClonedVoiceSynthesis(ssml: String, language: String, voiceProfile: String): ByteArray? {
        return try {
            val future = executor.submit(Callable {
                narrationMetrics.recordTtsLatency {
                    voiceStrategy.synthesize(ssml, language, voiceProfile)
                }
            })
            future.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        } catch (e: Exception) {
            log.warn("Cloned voice TTS failed: {}", e.message)
            narrationMetrics.recordTtsFailure()
            null
        }
    }

    private fun runWithCircuitBreakerAndRetries(ssml: String, language: String, voiceProfile: String): ByteArray? {
        var lastEx: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                val future = executor.submit(Callable {
                    narrationMetrics.recordTtsLatency {
                        voiceStrategy.synthesize(ssml, language, voiceProfile)
                    }
                })
                val result = circuitBreaker.executeCallable {
                    val raw = future.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                    raw ?: throw IllegalStateException("TTS synthesis returned null")
                }
                if (result != null && result.isNotEmpty()) {
                    narrationMetrics.recordTtsCharacters(ssml.length)
                    return result
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
    }
}
