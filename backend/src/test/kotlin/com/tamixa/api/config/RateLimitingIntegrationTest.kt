package com.tamixa.api.config

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource

/**
 * Integration test for rate limiting.
 * Re-enables the filter with a low quota; the default test profile keeps rate limiting disabled so other tests are not starved.
 */
@TestPropertySource(
    properties = [
        "app.rate-limit.enabled=true",
        "app.rate-limit.requests-per-minute=2",
    ]
)
class RateLimitingIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `exceeding rate limit returns 429`() {
        // With limit=2/min, make requests until we get 429
        var lastStatus = restTemplate.getForEntity("${ApiVersion.V1}/health", Map::class.java).statusCode
        repeat(10) {
            val r = restTemplate.getForEntity("${ApiVersion.V1}/health", Map::class.java)
            lastStatus = r.statusCode
            if (r.statusCode == HttpStatus.TOO_MANY_REQUESTS) return
        }
        assertThat(lastStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
