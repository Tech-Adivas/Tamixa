package com.tamixa.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RequestTracingFilterTest {

    @Test
    fun `sanitizeIncomingTraceId accepts uuid and common gateway ids`() {
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId(" 550e8400-e29b-41d4-a716-446655440000 "))
            .isEqualTo("550e8400-e29b-41d4-a716-446655440000")
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId("abc-123_ABC"))
            .isEqualTo("abc-123_ABC")
    }

    @Test
    fun `sanitizeIncomingTraceId rejects blank oversized and unsafe characters`() {
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId(null)).isNull()
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId("   ")).isNull()
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId("a".repeat(129))).isNull()
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId("evil\nid")).isNull()
        assertThat(RequestTracingFilter.sanitizeIncomingTraceId("x;drop")).isNull()
    }
}
