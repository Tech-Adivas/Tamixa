package com.tamixa.api.edu

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class LifeSkillChoiceControllerTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `GET counters without auth returns 401`() {
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/edu/life-skill-choices/counters?childId=1",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `GET counters with auth but unknown child returns 400`() {
        val email = "parent-lifeskill-${System.currentTimeMillis()}@test.com"
        val password = "password123"
        val registerBody = """{"email":"$email","password":"$password","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerResponse = restTemplate.postForEntity(
            "${ApiVersion.V1}/auth/register",
            HttpEntity(registerBody, jsonHeaders()),
            Map::class.java,
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val accessToken = (registerResponse.body as Map<String, Any>)["accessToken"] as String

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val response = restTemplate.exchange(
            "${ApiVersion.V1}/edu/life-skill-choices/counters?childId=999999999",
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            String::class.java,
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
