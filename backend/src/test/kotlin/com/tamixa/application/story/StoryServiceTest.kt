package com.tamixa.application.story

import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.StructuredStoryResult
import com.tamixa.application.port.TokenUsage
import com.tamixa.application.story.ModerationCategories
import com.tamixa.application.story.ModerationResult
import com.tamixa.application.story.StructuredStoryPayload
import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integration test for StoryService.generate() flow.
 * Mocks OpenAIPort to avoid real API calls; verifies full generate path saves story and returns 201.
 */
class StoryServiceTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: org.springframework.boot.test.web.client.TestRestTemplate

    @MockBean
    private lateinit var openAIPort: OpenAIPort

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}

    @Test
    fun `generate returns 201 when OpenAI mocked and parent registered`() {
        val email = "parent-gen-${System.currentTimeMillis()}@test.com"
        val password = "password123"

        // Register parent
        val registerBody = """{"email":"$email","password":"$password","acceptedTerms":true,"acceptedPrivacy":true,"acceptedParentalAttestation":true}"""
        val registerRes = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(registerRes.statusCode).isEqualTo(HttpStatus.CREATED)

        // Login
        val loginBody = """{"email":"$email","password":"$password"}"""
        val loginRes = restTemplate.exchange(
            "${ApiVersion.V1}/auth/login",
            HttpMethod.POST,
            HttpEntity(loginBody, jsonHeaders()),
            mapTypeRef
        )
        assertThat(loginRes.statusCode).isEqualTo(HttpStatus.OK)
        val token = loginRes.body!!["accessToken"] as String

        // Mock OpenAI
        val payload = StructuredStoryPayload(
            title = "The Fox and the Grapes",
            moral = "It is easy to despise what you cannot get.",
            storyText = "A fox saw some grapes. He tried to reach them but could not.",
            // Must match StoryValidation duration vs word count (~13 words → ~5 s at 150 wpm).
            estimatedDurationSeconds = 5
        )
        whenever(openAIPort.generateStructuredStory(any(), any(), any(), any()))
            .thenReturn(StructuredStoryResult(payload, TokenUsage(10, 50, 60)))
        whenever(openAIPort.getModerationResult(any()))
            .thenReturn(ModerationResult(safe = true, categories = ModerationCategories()))
        whenever(openAIPort.isContentSafe(any())).thenReturn(true)

        // Generate story
        val genBody = """{"age":6,"language":"ta","theme":"fox","childName":"Listener"}"""
        val genHeaders = HttpHeaders().apply {
            set("Authorization", "Bearer $token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
        }
        val genRes = restTemplate.exchange(
            "${ApiVersion.V1}/stories/generate",
            HttpMethod.POST,
            HttpEntity(genBody, genHeaders),
            mapTypeRef
        )

        assertThat(genRes.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(genRes.body).containsKey("id")
        assertThat(genRes.body).containsEntry("theme", "fox")
        assertThat(genRes.body).containsKey("status")
    }
}
