package com.araro.application.narration

import com.araro.application.narration.impl.TTSServiceImpl
import com.araro.infrastructure.observability.NarrationMetrics
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.Executors

@ExtendWith(MockitoExtension::class)
class TTSServiceTest {

    @Mock
    private lateinit var mockVoiceStrategy: VoiceSynthesisStrategy

    private val metrics = NarrationMetrics(SimpleMeterRegistry())
    private val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var ttsService: TTSServiceImpl

    private val ttsConcurrencyLimiter = TtsConcurrencyLimiter(maxConcurrent = 10, defaultPermitTimeoutMs = 30000L)

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        ttsService = TTSServiceImpl(
            mockVoiceStrategy,
            metrics,
            ttsConcurrencyLimiter,
            circuitBreakerRegistry,
            executor,
            timeoutSeconds = 5,
            maxRetries = 2
        )
    }

    @Test
    fun `synthesize returns bytes when strategy succeeds`() {
        val expectedBytes = byteArrayOf(1, 2, 3, 4, 5)
        `when`(mockVoiceStrategy.synthesize(anyString(), anyString(), anyString())).thenReturn(expectedBytes)
        val result = ttsService.synthesize("<speak>Hi</speak>", "en", "default")
        assertNotNull(result)
        assertEquals(5, result!!.size)
    }

    @Test
    fun `synthesize throws when strategy returns null`() {
        `when`(mockVoiceStrategy.synthesize(anyString(), anyString(), anyString())).thenReturn(null)
        val ex = assertThrows(IllegalStateException::class.java) {
            ttsService.synthesize("<speak>Hi</speak>", "en", "default")
        }
        assertEquals("TTS synthesis returned null", ex.message)
    }

    @Test
    fun `synthesize throws with real error when strategy throws`() {
        `when`(mockVoiceStrategy.synthesize(anyString(), anyString(), anyString()))
            .thenThrow(RuntimeException("Voice 'te-IN-Wavenet-A' does not exist"))
        val ex = assertThrows(IllegalStateException::class.java) {
            ttsService.synthesize("<speak>Hi</speak>", "en", "default")
        }
        assertNotNull(ex.message)
        assert(ex.message!!.contains("te-IN-Wavenet-A")) { "Expected error to contain voice name, got: ${ex.message}" }
    }
}
