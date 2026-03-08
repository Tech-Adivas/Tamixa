package com.araro

import com.araro.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class AuthConsentIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `register without consent returns 400 with ConsentRequired`() {
        val email = "noconsent-${System.currentTimeMillis()}@test.com"
        val body = """{"email":"$email","password":"password123","acceptedTerms":false,"acceptedPrivacy":false,"acceptedParentalAttestation":false}"""
        val res = restTemplate.postForEntity(
            "${ApiVersion.V1}/auth/register",
            HttpEntity<String>(body, jsonHeaders()),
            Map::class.java
        )
        assertThat(res.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        if (res.body?.get("message") != null) {
            assertThat((res.body!!["message"] as String)).containsIgnoringCase("consent")
        }
    }

    @Test
    fun `register without parental attestation returns 400`() {
        val email = "noattest-${System.currentTimeMillis()}@test.com"
        val body = """{"email":"$email","password":"password123","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":false}"""
        val res = restTemplate.postForEntity(
            "${ApiVersion.V1}/auth/register",
            HttpEntity<String>(body, jsonHeaders()),
            Map::class.java
        )
        assertThat(res.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        if (res.body?.get("message") != null) {
            val msg = (res.body!!["message"] as String).lowercase()
            assertThat(msg.contains("parental") || msg.contains("consent")).isTrue()
        }
    }
}
