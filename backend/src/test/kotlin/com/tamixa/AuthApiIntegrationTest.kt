package com.tamixa

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

class AuthApiIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}

    @Test
    fun `register then login returns tokens`() {
        val email = "parent-${System.currentTimeMillis()}@test.com"
        val password = "password123"

        val registerBody = """{"email":"$email","password":"$password","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(registerResponse.body).containsKey("accessToken")
        assertThat(registerResponse.body).containsKey("refreshToken")

        val loginBody = """{"email":"$email","password":"$password"}"""
        val loginResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/login",
            HttpMethod.POST,
            HttpEntity(loginBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.OK)
        val accessToken = loginResponse.body!!["accessToken"] as String

        val meHeaders = HttpHeaders().apply { set("Authorization", "Bearer $accessToken") }
        val meResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/me",
            HttpMethod.GET,
            HttpEntity<Void>(meHeaders),
            mapTypeRef
        )
        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(meResponse.body).containsEntry("email", email)
    }

    @Test
    fun `login with invalid credentials returns 401`() {
        val loginBody = """{"email":"nonexistent@test.com","password":"wrong"}"""
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/auth/login",
            HttpMethod.POST,
            HttpEntity(loginBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `health endpoint returns UP`() {
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/health",
            HttpMethod.GET,
            null,
            mapTypeRef
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).containsEntry("status", "UP")
    }

}
