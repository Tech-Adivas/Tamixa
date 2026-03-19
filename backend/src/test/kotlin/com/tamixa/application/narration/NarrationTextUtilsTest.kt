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
}
