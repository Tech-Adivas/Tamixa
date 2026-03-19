package com.tamixa.application.narration

import com.tamixa.application.port.narration.NarrationLLMResult
import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.application.narration.impl.NarrationFormatterServiceImpl
import com.tamixa.application.narration.impl.TokenUsageServiceImpl
import com.tamixa.domain.narration.ToneMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NarrationFormatterServiceTest {

    @Mock
    private lateinit var mockLlm: NarrationLLMPort

    private val tokenUsageService = TokenUsageServiceImpl(100000)
    private val rewriteLimiter = RewriteConcurrencyLimiter(4)
    private lateinit var formatter: NarrationFormatterServiceImpl

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        formatter = NarrationFormatterServiceImpl(mockLlm, tokenUsageService, rewriteLimiter, 1024)
    }

    @Test
    fun `formatNarration returns LLM result when within limit`() {
        val expectedScript = "Once upon a time, in a calm forest..."
        whenever(mockLlm.formatNarration(any(), any(), any(), any(), any()))
            .thenReturn(NarrationLLMResult(expectedScript, 100, 50))
        val result = formatter.formatNarration("Raw story", 5, ToneMode.CALM, "en")
        assertEquals(expectedScript, result.scriptText)
        assertEquals(100, result.promptTokens)
        assertEquals(50, result.completionTokens)
    }

    @Test
    fun `formatNarration returns original text when limit exceeded`() {
        val tokenUsage = TokenUsageServiceImpl(100)
        tokenUsage.recordAndCheckLimit(100)
        val formatterWithLimit = NarrationFormatterServiceImpl(mockLlm, tokenUsage, rewriteLimiter, 1024)
        val original = "Original story content"
        val result = formatterWithLimit.formatNarration(original, 5, ToneMode.CALM, "en")
        assertEquals(original, result.scriptText)
        assertEquals(0, result.promptTokens)
    }
}
