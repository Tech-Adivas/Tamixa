package com.tamixa.api.subscription

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integration tests for SubscriptionController: auth requirement and upgrade/cancel flows.
 */
class SubscriptionControllerTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}

    @Test
    fun `get usage without auth returns 401`() {
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription/usage",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `get current subscription without auth returns 401`() {
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `upgrade without auth returns 401`() {
        val body = """{"successUrl":"http://localhost/success","cancelUrl":"http://localhost/cancel"}"""
        val headers = HttpHeaders().apply {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
        }
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription/upgrade",
            HttpMethod.POST,
            HttpEntity(body, headers),
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `cancel without auth returns 401`() {
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription/cancel",
            HttpMethod.POST,
            HttpEntity.EMPTY,
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `get usage with auth returns 200 and usage fields`() {
        val email = "sub-usage-${System.currentTimeMillis()}@test.com"
        val password = "password123"
        val registerBody = """{"email":"$email","password":"$password","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val accessToken = registerResponse.body!!["accessToken"] as String

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription/usage",
            HttpMethod.GET,
            HttpEntity(null, headers),
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).containsKeys("month", "storiesUsed", "storiesLimit", "voiceUsed", "voiceLimit")
    }

    @Test
    fun `get current subscription with auth returns 200 and plan`() {
        val email = "sub-current-${System.currentTimeMillis()}@test.com"
        val password = "password123"
        val registerBody = """{"email":"$email","password":"$password","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val accessToken = registerResponse.body!!["accessToken"] as String

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription",
            HttpMethod.GET,
            HttpEntity(null, headers),
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).containsKeys("plan", "status", "provider", "isEntitledToUnlimitedStories")
    }

    @Test
    fun `cancel with auth returns 200`() {
        val email = "sub-cancel-${System.currentTimeMillis()}@test.com"
        val password = "password123"
        val registerBody = """{"email":"$email","password":"$password","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val accessToken = registerResponse.body!!["accessToken"] as String

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/subscription/cancel",
            HttpMethod.POST,
            HttpEntity(null, headers),
            Unit::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
