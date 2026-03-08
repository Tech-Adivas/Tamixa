package com.araro.application.narration

import com.araro.application.narration.impl.SSMLBuilderServiceImpl
import com.araro.domain.narration.ToneMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SSMLBuilderServiceImplTest {

    private val ssmlBuilder = SSMLBuilderServiceImpl()

    @Test
    fun `buildSSML wraps in speak and lang`() {
        val result = ssmlBuilder.buildSSML("Hello world.", "en", 5, ToneMode.CALM)
        assertTrue(result.contains("<speak"))
        assertTrue(result.contains("</speak>"))
        assertTrue(result.contains("xml:lang="))
        assertTrue(result.contains("en-US"))
    }

    @Test
    fun `buildSSML adds prosody rate`() {
        val result = ssmlBuilder.buildSSML("Bedtime story.", "en", 4, ToneMode.CALM)
        assertTrue(result.contains("rate=\"slow\""), "English uses slow for kid-friendly pace")
    }

    @Test
    fun `buildSSML adds break between paragraphs`() {
        val script = "First paragraph.\n\nSecond paragraph."
        val result = ssmlBuilder.buildSSML(script, "en", 7, ToneMode.EXPRESSIVE)
        assertTrue(result.contains("""<break time="300ms"/>"""), "English: 300ms per spec (relaxed pace)")
        assertTrue(result.contains("<p>"))
    }

    @Test
    fun `buildSSML maps language to BCP47`() {
        val result = ssmlBuilder.buildSSML("Hi", "hi", 6, ToneMode.CALM)
        assertTrue(result.contains("hi-IN"))
    }

    @Test
    fun `buildSSML escapes XML special characters`() {
        val script = "Story with <brackets> & ampersand"
        val result = ssmlBuilder.buildSSML(script, "en", 5, ToneMode.CALM)
        assertTrue(result.contains("&lt;"))
        assertTrue(result.contains("&amp;"))
    }

    @Test
    fun `buildSSML uses language-specific break timing for en`() {
        val script = "First.\n\nSecond."
        val resultEn = ssmlBuilder.buildSSML(script, "en", 3, ToneMode.CALM)
        assertTrue(resultEn.contains("""time="300ms""""), "English: 300ms per spec (smooth flow)")
    }

    @Test
    fun `buildSSML uses language-specific tuning Tamil slow 300ms`() {
        val script = "First.\n\nSecond."
        val resultTa = ssmlBuilder.buildSSML(script, "ta", 5, ToneMode.CALM)
        assertTrue(resultTa.contains("""time="300ms""""), "Tamil: 300ms per spec (smooth flow)")
        assertTrue(resultTa.contains("rate=\"slow\""))
    }

    @Test
    fun `buildSSML uses language-specific tuning Hindi slow 300ms`() {
        val script = "First.\n\nSecond."
        val resultHi = ssmlBuilder.buildSSML(script, "hi", 7, ToneMode.CALM)
        assertTrue(resultHi.contains("""time="300ms""""), "Hindi: 300ms per spec (smooth flow)")
        assertTrue(resultHi.contains("rate=\"slow\""))
    }

    @Test
    fun `buildSSML uses language-specific tuning Kannada slow 300ms`() {
        val script = "First.\n\nSecond."
        val resultKn = ssmlBuilder.buildSSML(script, "kn", 6, ToneMode.CALM)
        assertTrue(resultKn.contains("""time="300ms""""), "Kannada: 300ms per spec (smooth flow)")
    }

    @Test
    fun `buildSSML uses language-specific rate for Hindi`() {
        val result = ssmlBuilder.buildSSML("Hello", "hi", 7, ToneMode.CALM)
        assertTrue(result.contains("rate=\"slow\""), "Hindi uses slow for kid-friendly pace")
    }

    @Test
    fun `buildSSML produces valid SSML structure`() {
        val result = ssmlBuilder.buildSSML("Test.", "en", 6, ToneMode.EXPRESSIVE)
        assertTrue(result.startsWith("<speak"))
        assertTrue(result.endsWith("</speak>"))
        assertTrue(result.contains("<lang"))
        assertTrue(result.contains("<prosody"))
    }
}
