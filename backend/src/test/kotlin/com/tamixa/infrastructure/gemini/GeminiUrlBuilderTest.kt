package com.tamixa.infrastructure.gemini

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeminiUrlBuilderTest {

    @Test
    fun `generateContentUrl google-ai`() {
        val u = GeminiUrlBuilder.generateContentUrl(
            "https://generativelanguage.googleapis.com",
            "gemini-2.5-flash",
            "KEY",
            "google-ai",
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=KEY",
            u,
        )
    }

    @Test
    fun `generateContentUrl vertex-publishers`() {
        val u = GeminiUrlBuilder.generateContentUrl(
            "https://aiplatform.googleapis.com",
            "gemini-2.5-flash-lite",
            "KEY",
            "vertex-publishers",
        )
        assertEquals(
            "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash-lite:generateContent?key=KEY",
            u,
        )
    }

    @Test
    fun `modelsListUrl vertex-publishers`() {
        val u = GeminiUrlBuilder.modelsListUrl(
            "https://aiplatform.googleapis.com",
            "KEY",
            "vertex",
        )
        assertEquals(
            "https://aiplatform.googleapis.com/v1/publishers/google/models?key=KEY",
            u,
        )
    }
}
