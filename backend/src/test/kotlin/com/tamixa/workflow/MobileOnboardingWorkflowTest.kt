package com.tamixa.workflow

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
 * End-to-end workflow test for Mobile Onboarding Flow (Requirements 2, 14).
 * 
 * Tests the complete onboarding sequence:
 * 1. User registration
 * 2. Profile boot (initial data load)
 * 3. Voice profile creation (optional)
 * 4. Avatar upload (optional)
 * 5. Onboarding completion persistence
 * 
 * Success Criteria:
 * - User can complete registration and authentication
 * - Profile data is accessible after registration
 * - Voice and avatar features are optional (can skip)
 * - Onboarding state persists across sessions
 */
class MobileOnboardingWorkflowTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}

    @Test
    fun `complete onboarding flow - register, profile boot, skip voice and avatar`() {
        // Step 1: Register new user (Hook screen → Demo screen)
        val email = "onboarding-${System.currentTimeMillis()}@test.com"
        val password = "SecurePass123!"
        
        val registerBody = """
            {
                "email": "$email",
                "password": "$password",
                "acceptedTerms": true,
                "acceptedPrivacy": true,
                "acceptedParentalAttestation": true
            }
        """.trimIndent()
        
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(registerResponse.body).containsKey("accessToken")
        assertThat(registerResponse.body).containsKey("refreshToken")
        
        val accessToken = registerResponse.body!!["accessToken"] as String
        val authHeaders = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        
        // Step 2: Profile boot - load initial user data (after Demo screen)
        val profileBootResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(profileBootResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(profileBootResponse.body).containsKey("parent")
        assertThat(profileBootResponse.body).containsKey("children")
        
        val parent = profileBootResponse.body!!["parent"] as Map<*, *>
        assertThat(parent["email"]).isEqualTo(email)
        
        // Step 3: Interactive Story Preview (new onboarding step)
        // User views interactive story preview - no API call needed, just UI
        // This step demonstrates branching stories to the user
        
        // Step 4: Voice Invitation - user can skip (optional)
        // In real flow, user would either upload voice or skip
        // We test the skip path here (no API call needed)
        
        // Step 5: Avatar Invitation - user can skip (optional)
        // In real flow, user would either upload avatar or skip
        // We test the skip path here (no API call needed)
        
        // Step 6: Verify onboarding completion - user can access main app
        val meResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/me",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(meResponse.body).containsEntry("email", email)
        
        // Verify user can access protected resources (dashboard data)
        val homeResponse = restTemplate.exchange(
            "${ApiVersion.V1}/home",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(homeResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(homeResponse.body).containsKey("spotlight")
        assertThat(homeResponse.body).containsKey("categories")
    }
    
    @Test
    fun `onboarding flow with voice profile creation`() {
        // Step 1: Register and authenticate
        val email = "voice-onboarding-${System.currentTimeMillis()}@test.com"
        val password = "SecurePass123!"
        
        val registerBody = """
            {
                "email": "$email",
                "password": "$password",
                "acceptedTerms": true,
                "acceptedPrivacy": true,
                "acceptedParentalAttestation": true
            }
        """.trimIndent()
        
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val accessToken = registerResponse.body!!["accessToken"] as String
        val authHeaders = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        
        // Step 2: Create child profile (required for voice profile)
        val childBody = """
            {
                "name": "TestChild",
                "age": 6,
                "gender": "other"
            }
        """.trimIndent()
        
        val childResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children",
            HttpMethod.POST,
            HttpEntity(childBody, authHeaders.apply { contentType = org.springframework.http.MediaType.APPLICATION_JSON }),
            mapTypeRef
        )
        
        assertThat(childResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(childResponse.body).containsKey("id")
        val childId = (childResponse.body!!["id"] as Number).toLong()
        
        // Step 3: Voice profile creation during onboarding
        // Note: Actual voice upload would require multipart/form-data
        // This test verifies the endpoint is accessible
        val voiceListResponse = restTemplate.exchange(
            "${ApiVersion.V1}/voice-profiles",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            object : ParameterizedTypeReference<List<Map<String, Any>>>() {}
        )
        
        assertThat(voiceListResponse.statusCode).isEqualTo(HttpStatus.OK)
        // Initially empty for new user
        assertThat(voiceListResponse.body).isEmpty()
        
        // Step 4: Complete onboarding and verify access
        val profileBootResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(profileBootResponse.statusCode).isEqualTo(HttpStatus.OK)
        val children = profileBootResponse.body!!["children"] as List<*>
        assertThat(children).hasSize(1)
    }
    
    @Test
    fun `onboarding flow - navigation between steps maintains state`() {
        // Test that user can navigate back and forth during onboarding
        // without losing progress
        
        val email = "nav-onboarding-${System.currentTimeMillis()}@test.com"
        val password = "SecurePass123!"
        
        // Step 1: Register
        val registerBody = """
            {
                "email": "$email",
                "password": "$password",
                "acceptedTerms": true,
                "acceptedPrivacy": true,
                "acceptedParentalAttestation": true
            }
        """.trimIndent()
        
        val registerResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(registerBody, jsonHeaders()),
            mapTypeRef
        )
        
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val accessToken = registerResponse.body!!["accessToken"] as String
        val authHeaders = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        
        // Step 2: Update profile (simulating user filling in details)
        val updateProfileBody = """
            {
                "name": "Test Parent",
                "nickname": "TestNick"
            }
        """.trimIndent()
        
        val updateResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me",
            HttpMethod.PUT,
            HttpEntity(updateProfileBody, authHeaders.apply { contentType = org.springframework.http.MediaType.APPLICATION_JSON }),
            mapTypeRef
        )
        
        assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Step 3: Verify profile persists (user navigates back to check)
        val profileCheckResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(profileCheckResponse.statusCode).isEqualTo(HttpStatus.OK)
        val parent = profileCheckResponse.body!!["parent"] as Map<*, *>
        assertThat(parent["name"]).isEqualTo("Test Parent")
        assertThat(parent["nickname"]).isEqualTo("TestNick")
        
        // Step 4: Continue to next step - verify state maintained
        val meResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/me",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(meResponse.body).containsEntry("email", email)
    }
    
    @Test
    fun `onboarding flow - error handling for invalid registration`() {
        // Test error scenarios during onboarding
        
        // Invalid email format
        val invalidEmailBody = """
            {
                "email": "not-an-email",
                "password": "SecurePass123!",
                "acceptedTerms": true,
                "acceptedPrivacy": true,
                "acceptedParentalAttestation": true
            }
        """.trimIndent()
        
        val invalidEmailResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(invalidEmailBody, jsonHeaders()),
            mapTypeRef
        )
        
        assertThat(invalidEmailResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        
        // Missing required consent
        val missingConsentBody = """
            {
                "email": "valid@test.com",
                "password": "SecurePass123!",
                "acceptedTerms": false,
                "acceptedPrivacy": true,
                "acceptedParentalAttestation": true
            }
        """.trimIndent()
        
        val missingConsentResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/register",
            HttpMethod.POST,
            HttpEntity(missingConsentBody, jsonHeaders()),
            mapTypeRef
        )
        
        assertThat(missingConsentResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
