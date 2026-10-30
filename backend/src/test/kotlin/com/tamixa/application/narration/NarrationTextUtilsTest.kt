package com.tamixa.application.narration

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NarrationTextUtilsTest {

    @Test
    fun `stripRemainingMarkers removes Warm tone`() {
        val input = "The forest was peaceful. [Warm tone] A small bird sang."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertFalse(result.contains("Warm tone"), "Warm tone should be stripped; got: $result")
        assertTrue(result.contains("forest was peaceful"), result)
        assertTrue(result.contains("small bird sang"), result)
    }

    @Test
    fun `stripRemainingMarkers removes multiple markers`() {
        val input = "Once. [Warm tone] [Happy tone] [Pause 1s] the sun rose."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertFalse(result.contains("Warm tone"), result)
        assertFalse(result.contains("Happy tone"), result)
        assertFalse(result.contains("Pause 1s"), result)
        assertTrue(result.contains("Once."), result)
        assertTrue(result.contains("sun rose"), result)
    }

    @Test
    fun `stripRemainingMarkers removes Pause markers`() {
        val input = "Hello. [Pause 500ms] World."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertFalse(result.contains("Pause 500ms"), result)
        assertTrue(result.contains("Hello."), result)
        assertTrue(result.contains("World."), result)
    }

    @Test
    fun `stripRemainingMarkers leaves normal text unchanged`() {
        val input = "Once upon a time there was a brave little fox."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertTrue(result == input, "Normal text should be unchanged; got: $result")
    }

    @Test
    fun `stripRemainingMarkers handles blank input`() {
        assertTrue(NarrationTextUtils.stripRemainingMarkers("").isEmpty())
        assertTrue(NarrationTextUtils.stripRemainingMarkers("   ").isEmpty())
        assertTrue(NarrationTextUtils.stripRemainingMarkers("\t\n").isEmpty())
    }

    @Test
    fun `stripRemainingMarkers removes typo Meduim pacing and decimal pause`() {
        val input = "Hello. [Meduim pacing] World. [Pause 1.5s] End."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertFalse(result.contains("Meduim"), result)
        assertFalse(result.contains("Pause 1.5s"), result)
        assertTrue(result.contains("Hello."), result)
        assertTrue(result.contains("World."), result)
        assertTrue(result.contains("End."), result)
    }

    @Test
    fun `stripRemainingMarkers removes Thoughtful tone`() {
        val input = "She paused. [Thoughtful tone] Then she smiled."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertFalse(result.contains("Thoughtful"), result)
        assertTrue(result.contains("She paused."), result)
        assertTrue(result.contains("smiled."), result)
    }

    @Test
    fun `split keeps sentences whole and loses no text`() {
        val text = "First sentence. Second one!  Third?\nमैं घर गया। फिर सो गया। " + "word ".repeat(300)
        val parts = NarrationTextUtils.splitForSpeech(text, 100)

        org.assertj.core.api.Assertions.assertThat(parts).allSatisfy { org.assertj.core.api.Assertions.assertThat(it.length).isLessThanOrEqualTo(100) }
        org.assertj.core.api.Assertions.assertThat(parts.joinToString(" ")).isEqualTo(text.replace(Regex("\\s+"), " ").trim())
        org.assertj.core.api.Assertions.assertThat(parts[0]).startsWith("First sentence. Second one! Third? मैं घर गया। फिर सो गया।")
        org.assertj.core.api.Assertions.assertThat(NarrationTextUtils.splitForSpeech("x".repeat(250), 100)).containsExactly("x".repeat(100), "x".repeat(100), "x".repeat(50))
        org.assertj.core.api.Assertions.assertThat(NarrationTextUtils.splitForSpeech("  short  text ", 100)).containsExactly("short text")
    }

    @Test
    fun `split keeps paragraphs apart for the voice to pause and ends parts at paragraph ends`() {
        val text = "Para one first. Para one second.\n\nPara two only.\n\n\n  Para three here.  "
        org.assertj.core.api.Assertions.assertThat(NarrationTextUtils.splitForSpeech(text, 1000))
            .containsExactly("Para one first. Para one second.\n\nPara two only.\n\nPara three here.")
        // 40 chars: paragraphs 1+2 don't fit together -> a new part at the paragraph end, not mid-paragraph.
        org.assertj.core.api.Assertions.assertThat(NarrationTextUtils.splitForSpeech(text, 40))
            .containsExactly("Para one first. Para one second.", "Para two only.\n\nPara three here.")
    }
}
