package com.tamixa.application.narration

import com.tamixa.application.narration.impl.SSMLBuilderServiceImpl
import com.tamixa.domain.narration.ToneMode
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
    fun `buildSSML uses natural pace by default`() {
        val result = ssmlBuilder.buildSSML("Bedtime story.", "en", 4, ToneMode.CALM)
        assertTrue(result.contains("rate=\"100%\""), "Default: natural pace matches Cloud samples")
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
        assertTrue(resultEn.contains("""time="300ms""""), "English: 300ms per language tuning")
    }

    @Test
    fun `buildSSML uses natural rate for Tamil`() {
        val script = "First.\n\nSecond."
        val resultTa = ssmlBuilder.buildSSML(script, "ta", 5, ToneMode.CALM)
        assertTrue(resultTa.contains("rate=\"100%\""), "Natural pace: no slowdown")
    }

    @Test
    fun `buildSSML uses natural rate for Hindi`() {
        val script = "First.\n\nSecond."
        val resultHi = ssmlBuilder.buildSSML(script, "hi", 7, ToneMode.CALM)
        assertTrue(resultHi.contains("rate=\"100%\""), "Natural pace: no slowdown")
    }

    @Test
    fun `buildSSML uses kid-friendly slow rate when natural pace disabled`() {
        val expressiveBuilder = SSMLBuilderServiceImpl(false)
        val result = expressiveBuilder.buildSSML("Hello", "hi", 7, ToneMode.CALM)
        assertTrue(result.contains("rate=\"slow\""), "Expressive mode: Hindi uses slow for kid-friendly pace")
    }

    @Test
    fun `buildSSML converts inline pause markers to SSML break`() {
        val script = "Hello. [Pause 500ms] World."
        val result = ssmlBuilder.buildSSML(script, "en", 5, ToneMode.CALM)
        assertTrue(result.contains("""<break time="500ms"/>"""), "Pause 500ms should become break tag")
        assertTrue(!result.contains("Pause 500ms"), "Pause marker text should not appear in SSML")
    }

    @Test
    fun `buildSSML strips tone markers so TTS does not read them`() {
        val script = "Once upon a time [Happy tone] there was a brave little fox."
        val result = ssmlBuilder.buildSSML(script, "en", 5, ToneMode.CALM)
        assertTrue(!result.contains("Happy tone"), "Tone marker should be stripped")
        assertTrue(result.contains("Once upon a time"), "Story text should remain")
        assertTrue(result.contains("brave little fox"), "Story text should remain")
    }

    @Test
    fun `buildSSML strips Warm tone marker`() {
        val script = "The forest was peaceful. [Warm tone] A small bird sang."
        val result = ssmlBuilder.buildSSML(script, "en", 5, ToneMode.CALM)
        assertTrue(!result.contains("Warm tone"), "Warm tone marker should be stripped; got: $result")
        assertTrue(result.contains("forest was peaceful"), "Story text should remain")
        assertTrue(result.contains("small bird sang"), "Story text should remain")
    }

    @Test
    fun `buildSSML strips all marker types and converts Pause 1s`() {
        val script = "Once. [Warm tone] [Happy tone] [Pause 1s] the sun rose."
        val result = ssmlBuilder.buildSSML(script, "en", 5, ToneMode.CALM)
        assertTrue(!result.contains("Warm tone"), "Warm tone should be stripped")
        assertTrue(!result.contains("Happy tone"), "Happy tone should be stripped")
        assertTrue(!result.contains("Pause 1s"), "Pause 1s text should not appear")
        assertTrue(result.contains("""<break time="1s"/>"""), "Pause 1s should become break tag")
        assertTrue(result.contains("Once."), "Story text should remain")
        assertTrue(result.contains("sun rose"), "Story text should remain")
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
