package com.araro.api.playback

import com.araro.IntegrationTestBase
import com.araro.api.ApiVersion
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
 * Integration test for PlaybackPositionController.
 */
class PlaybackPositionControllerTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}

    @Test
    fun `save position with auth returns 200`() {
        val email = "parent-playback-${System.currentTimeMillis()}@test.com"
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
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            set("Authorization", "Bearer $accessToken")
        }
        val saveBody = """{"storyId":1,"storySource":"generated","positionSeconds":120}"""
        val saveResponse = restTemplate.exchange(
            "${ApiVersion.V1}/playback/position",
            HttpMethod.POST,
            HttpEntity(saveBody, headers),
            Unit::class.java
        )
        assertThat(saveResponse.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `save position without auth returns 401`() {
        val headers = HttpHeaders().apply { contentType = org.springframework.http.MediaType.APPLICATION_JSON }
        val body = """{"storyId":1,"storySource":"generated","positionSeconds":120}"""
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/playback/position",
            HttpMethod.POST,
            HttpEntity(body, headers),
            Unit::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `get recent without auth returns 401`() {
        val response = restTemplate.getForEntity(
            "${ApiVersion.V1}/playback/recent?limit=5",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
