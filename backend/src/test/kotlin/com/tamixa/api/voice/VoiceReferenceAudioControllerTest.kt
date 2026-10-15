package com.tamixa.api.voice

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
 * Integration test for GET /voice/{id}/reference-audio (parent playback of a saved recording).
 * Reference audio storage is S3-only, so the happy path is covered manually; this test guards auth and ownership.
 */
class VoiceReferenceAudioControllerTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}

    @Test
    fun `reference audio without auth returns 401`() {
        val response = restTemplate.getForEntity(
            "${ApiVersion.V1}/voice/1/reference-audio",
            ByteArray::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `reference audio for profile not owned by caller returns 404`() {
        val email = "parent-voice-ref-${System.currentTimeMillis()}@test.com"
        val registerBody = """{"email":"$email","password":"password123","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val accessToken = registerResponse.body!!["accessToken"] as String

        val headers = HttpHeaders().apply { set("Authorization", "Bearer $accessToken") }
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/voice/999999999/reference-audio",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            ByteArray::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
