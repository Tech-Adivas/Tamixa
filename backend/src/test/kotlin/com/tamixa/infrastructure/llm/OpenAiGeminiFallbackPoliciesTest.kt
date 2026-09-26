package com.tamixa.infrastructure.llm

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class OpenAiGeminiFallbackPoliciesTest {

    @Test
    fun `429 triggers fallback`() {
        val e = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too Many Requests",
            HttpHeaders.EMPTY,
            ByteArray(0),
            null,
        )
        assertTrue(OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(e))
    }

    @Test
    fun `401 does not trigger fallback`() {
        val e = HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            HttpHeaders.EMPTY,
            ByteArray(0),
            null,
        )
        assertFalse(OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(e))
    }

    @Test
    fun `wrapped insufficient_quota message triggers fallback`() {
        val inner = IllegalStateException(
            """429 Too Many Requests: "insufficient_quota"""",
            HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                ByteArray(0),
                null,
            ),
        )
        assertTrue(OpenAiGeminiFallbackPolicies.shouldTryGeminiAfterOpenAiFailure(inner))
    }

    @Test
    fun `truthy property parsing`() {
        assertTrue(OpenAiGeminiFallbackPolicies.isTruthyProperty("true"))
        assertTrue(OpenAiGeminiFallbackPolicies.isTruthyProperty("YES"))
        assertFalse(OpenAiGeminiFallbackPolicies.isTruthyProperty("false"))
        assertFalse(OpenAiGeminiFallbackPolicies.isTruthyProperty(null))
    }
}
