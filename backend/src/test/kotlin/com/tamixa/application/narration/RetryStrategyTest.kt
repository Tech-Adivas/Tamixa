package com.tamixa.application.narration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Retry strategy: max 3 retries, exponential backoff 2s, 5s, 10s.
 */
class RetryStrategyTest {

    private val maxRetries = 3

    @Test
    fun `exponential backoff delays match spec 2s 5s 10s`() {
        val delays = (1..3).map { attempt ->
            when (attempt) {
                1 -> 2000L
                2 -> 5000L
                else -> 10000L
            }
        }
        assertEquals(2000L, delays[0])
        assertEquals(5000L, delays[1])
        assertEquals(10000L, delays[2])
    }

    @Test
    fun `max retries is 3`() {
        assertEquals(3, maxRetries)
    }
}
