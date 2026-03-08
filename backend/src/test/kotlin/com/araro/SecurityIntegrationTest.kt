package com.araro

import com.araro.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

/**
 * Security tests: protected endpoints require authentication;
 * rate limiting and CORS are configured in SecurityConfig.
 */
class SecurityIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `accessing protected endpoint without token returns 401`() {
        val response = restTemplate.getForEntity(
            "${ApiVersion.V1}/children",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `accessing story list without token returns 401`() {
        val response = restTemplate.getForEntity(
            "${ApiVersion.V1}/stories",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `actuator health is accessible without auth`() {
        val response = restTemplate.getForEntity(
            "/actuator/health",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
