package com.tamixa.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NarrationTextUtilsTest {

    @Test
    fun stripRemovesPauseToneAndTypoPacing() {
        val input = "Start. [pause 300ms] [Thoughtful tone] [Meduim pacing] The end."
        val result = NarrationTextUtils.stripRemainingMarkers(input)
        assertFalse(result.lowercase().contains("[pause"))
        assertFalse(result.contains("Thoughtful"))
        assertFalse(result.contains("Meduim"))
        assertTrue(result.contains("Start."))
        assertTrue(result.contains("The end."))
    }
}
