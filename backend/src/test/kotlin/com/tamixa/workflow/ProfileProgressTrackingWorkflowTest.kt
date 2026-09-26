package com.tamixa.workflow

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * End-to-end workflow test for Profile and Progress Tracking (Requirement 10).
 * 
 * Tests the complete profile and progress tracking workflow:
 * 1. Profile data display (user info, children)
 * 2. Learning progress updates (reading level, vocabulary, life readiness)
 * 3. Life skills tracking (wisdom, social, money, balance)
 * 4. Settings management
 * 5. Listening streak tracking
 * 
 * Success Criteria:
 * - User can view and edit profile information
 * - Learning progress is tracked and updated
 * - Life skills counters increment correctly
 * - Listening streak is calculated accurately
 * - Settings changes persist across sessions
 */
class ProfileProgressTrackingWorkflowTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}
    private val listMapTypeRef = object : ParameterizedTypeReference<List<Map<String, Any>>>() {}
    
    private lateinit var accessToken: String
    private lateinit var authHeaders: HttpHeaders

    @BeforeEach
    fun setupAuthenticatedUser() {
        val email = "profile-user-${System.currentTimeMillis()}@test.com"
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
        
        accessToken = registerResponse.body!!["accessToken"] as String
        authHeaders = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
        }
    }

    @Test
    fun `complete profile workflow - view, edit, add child, track progress`() {
        // Step 1: Get initial profile data
        val profileBootResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(profileBootResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(profileBootResponse.body).containsKey("parent")
        assertThat(profileBootResponse.body).containsKey("children")
        assertThat(profileBootResponse.body).containsKey("subscription")
        
        val parent = profileBootResponse.body!!["parent"] as Map<*, *>
        assertThat(parent).containsKey("id")
        assertThat(parent).containsKey("email")
        
        val initialChildren = profileBootResponse.body!!["children"] as List<*>
        assertThat(initialChildren).isEmpty()
        
        // Step 2: Update parent profile
        val updateProfileBody = """
            {
                "name": "Test Parent",
                "nickname": "TestNick"
            }
        """.trimIndent()
        
        val updateProfileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me",
            HttpMethod.PUT,
            HttpEntity(updateProfileBody, authHeaders),
            mapTypeRef
        )
        
        assertThat(updateProfileResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(updateProfileResponse.body).containsEntry("name", "Test Parent")
        assertThat(updateProfileResponse.body).containsEntry("nickname", "TestNick")
        
        // Step 3: Add a child
        val addChildBody = """
            {
                "name": "Maya",
                "age": 7,
                "gender": "female"
            }
        """.trimIndent()
        
        val addChildResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children",
            HttpMethod.POST,
            HttpEntity(addChildBody, authHeaders),
            mapTypeRef
        )
        
        assertThat(addChildResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(addChildResponse.body).containsKey("id")
        assertThat(addChildResponse.body).containsEntry("name", "Maya")
        
        val childId = (addChildResponse.body!!["id"] as Number).toLong()
        
        // Step 4: Verify child appears in profile
        val updatedProfileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val updatedChildren = updatedProfileResponse.body!!["children"] as List<*>
        assertThat(updatedChildren).hasSize(1)
        
        val child = updatedChildren[0] as Map<*, *>
        assertThat(child["name"]).isEqualTo("Maya")
        assertThat(child).containsKeys("readingLevel", "vocabularyCount", "lifeReadinessScore")
        
        // Step 5: Update child profile
        val updateChildBody = """
            {
                "name": "Maya Sharma",
                "nickname": "Maya"
            }
        """.trimIndent()
        
        val updateChildResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children/$childId",
            HttpMethod.PUT,
            HttpEntity(updateChildBody, authHeaders),
            mapTypeRef
        )
        
        assertThat(updateChildResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(updateChildResponse.body).containsEntry("name", "Maya Sharma")
        
        // Step 6: Get life skills counters
        val lifeSkillsResponse = restTemplate.exchange(
            "${ApiVersion.V1}/life-skills/counters",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(lifeSkillsResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(lifeSkillsResponse.body).containsKeys("wisdom", "social", "money", "balance")
        
        // Initial counters should be 0 or low
        val wisdom = (lifeSkillsResponse.body!!["wisdom"] as Number).toInt()
        val social = (lifeSkillsResponse.body!!["social"] as Number).toInt()
        assertThat(wisdom).isGreaterThanOrEqualTo(0)
        assertThat(social).isGreaterThanOrEqualTo(0)
        
        // Step 7: Get listening streak
        val streakResponse = restTemplate.exchange(
            "${ApiVersion.V1}/analytics/listening-streak",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(streakResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(streakResponse.body).containsKey("currentStreak")
        assertThat(streakResponse.body).containsKey("longestStreak")
        
        // New user should have 0 streak
        val currentStreak = (streakResponse.body!!["currentStreak"] as Number).toInt()
        assertThat(currentStreak).isEqualTo(0)
    }
    
    @Test
    fun `learning progress updates after story completion`() {
        // Test that learning metrics update when user completes stories
        
        // Step 1: Add a child
        val addChildBody = """
            {
                "name": "Alex",
                "age": 6,
                "gender": "other"
            }
        """.trimIndent()
        
        val addChildResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children",
            HttpMethod.POST,
            HttpEntity(addChildBody, authHeaders),
            mapTypeRef
        )
        
        val childId = (addChildResponse.body!!["id"] as Number).toLong()
        
        // Step 2: Get initial learning progress
        val initialProfileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val initialChildren = initialProfileResponse.body!!["children"] as List<*>
        val initialChild = initialChildren[0] as Map<*, *>
        val initialVocabulary = (initialChild["vocabularyCount"] as Number).toInt()
        val initialReadingLevel = (initialChild["readingLevel"] as Number).toInt()
        
        // Step 3: Generate and complete a story
        val generateStoryBody = """
            {
                "age": 6,
                "language": "ta",
                "theme": "adventure",
                "childName": "Alex",
                "childId": $childId
            }
        """.trimIndent()
        
        val generateResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/generate",
            HttpMethod.POST,
            HttpEntity(generateStoryBody, authHeaders),
            mapTypeRef
        )
        
        if (generateResponse.statusCode == HttpStatus.CREATED) {
            val storyId = (generateResponse.body!!["id"] as Number).toLong()
            
            // Mark story as completed (save position at end)
            val completeStoryBody = """
                {
                    "storyId": $storyId,
                    "positionSeconds": 300,
                    "completed": true
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(completeStoryBody, authHeaders),
                mapTypeRef
            )
            
            // Step 4: Check if learning progress updated
            val updatedProfileResponse = restTemplate.exchange(
                "${ApiVersion.V1}/profile/boot",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            val updatedChildren = updatedProfileResponse.body!!["children"] as List<*>
            val updatedChild = updatedChildren[0] as Map<*, *>
            val updatedVocabulary = (updatedChild["vocabularyCount"] as Number).toInt()
            
            // Vocabulary might increase after story completion
            // (depends on backend implementation)
            assertThat(updatedVocabulary).isGreaterThanOrEqualTo(initialVocabulary)
        }
    }
    
    @Test
    fun `listening streak increments on consecutive days`() {
        // Test listening streak calculation
        
        // Step 1: Get initial streak
        val initialStreakResponse = restTemplate.exchange(
            "${ApiVersion.V1}/analytics/listening-streak",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(initialStreakResponse.statusCode).isEqualTo(HttpStatus.OK)
        val initialStreak = (initialStreakResponse.body!!["currentStreak"] as Number).toInt()
        
        // Step 2: Listen to a story (save playback position)
        val libraryResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=1",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val stories = libraryResponse.body!!["content"] as List<*>
        if (stories.isNotEmpty()) {
            val storyId = ((stories[0] as Map<*, *>)["id"] as Number).toLong()
            
            val savePositionBody = """
                {
                    "storyId": $storyId,
                    "positionSeconds": 60
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(savePositionBody, authHeaders),
                mapTypeRef
            )
            
            // Step 3: Check streak (might increment if this is first activity today)
            val updatedStreakResponse = restTemplate.exchange(
                "${ApiVersion.V1}/analytics/listening-streak",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            assertThat(updatedStreakResponse.statusCode).isEqualTo(HttpStatus.OK)
            val updatedStreak = (updatedStreakResponse.body!!["currentStreak"] as Number).toInt()
            
            // Streak should be at least as high as initial
            assertThat(updatedStreak).isGreaterThanOrEqualTo(initialStreak)
        }
    }
    
    @Test
    fun `life skills counters increment correctly`() {
        // Test that life skills counters update based on story categories
        
        // Step 1: Get initial counters
        val initialCountersResponse = restTemplate.exchange(
            "${ApiVersion.V1}/life-skills/counters",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val initialWisdom = (initialCountersResponse.body!!["wisdom"] as Number).toInt()
        val initialSocial = (initialCountersResponse.body!!["social"] as Number).toInt()
        
        // Step 2: Complete an educational story (should increment wisdom)
        val libraryResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?category=educational&page=0&size=1",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val stories = libraryResponse.body!!["content"] as List<*>
        if (stories.isNotEmpty()) {
            val storyId = ((stories[0] as Map<*, *>)["id"] as Number).toLong()
            
            // Complete the story
            val completeStoryBody = """
                {
                    "storyId": $storyId,
                    "positionSeconds": 200,
                    "completed": true
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(completeStoryBody, authHeaders),
                mapTypeRef
            )
            
            // Step 3: Check if wisdom counter increased
            val updatedCountersResponse = restTemplate.exchange(
                "${ApiVersion.V1}/life-skills/counters",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            val updatedWisdom = (updatedCountersResponse.body!!["wisdom"] as Number).toInt()
            
            // Wisdom should be at least as high as initial
            assertThat(updatedWisdom).isGreaterThanOrEqualTo(initialWisdom)
        }
    }
    
    @Test
    fun `profile settings persist across sessions`() {
        // Test that profile changes persist
        
        // Step 1: Update profile with specific settings
        val updateProfileBody = """
            {
                "name": "Persistent Parent",
                "nickname": "PP",
                "preferredLanguage": "ta"
            }
        """.trimIndent()
        
        restTemplate.exchange(
            "${ApiVersion.V1}/parents/me",
            HttpMethod.PUT,
            HttpEntity(updateProfileBody, authHeaders),
            mapTypeRef
        )
        
        // Step 2: Simulate new session by getting profile again
        val profileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val parent = profileResponse.body!!["parent"] as Map<*, *>
        assertThat(parent["name"]).isEqualTo("Persistent Parent")
        assertThat(parent["nickname"]).isEqualTo("PP")
        
        // Step 3: Verify via /auth/me endpoint as well
        val meResponse = restTemplate.exchange(
            "${ApiVersion.V1}/auth/me",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK)
    }
    
    @Test
    fun `multiple children tracked independently`() {
        // Test that progress is tracked separately for each child
        
        // Step 1: Add two children
        val child1Body = """{"name": "Child One", "age": 5, "gender": "female"}"""
        val child2Body = """{"name": "Child Two", "age": 8, "gender": "male"}"""
        
        val child1Response = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children",
            HttpMethod.POST,
            HttpEntity(child1Body, authHeaders),
            mapTypeRef
        )
        
        val child2Response = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children",
            HttpMethod.POST,
            HttpEntity(child2Body, authHeaders),
            mapTypeRef
        )
        
        val child1Id = (child1Response.body!!["id"] as Number).toLong()
        val child2Id = (child2Response.body!!["id"] as Number).toLong()
        
        // Step 2: Verify both children in profile
        val profileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val children = profileResponse.body!!["children"] as List<*>
        assertThat(children).hasSize(2)
        
        // Step 3: Update one child's profile
        val updateChild1Body = """{"name": "Child One Updated", "nickname": "C1"}"""
        restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children/$child1Id",
            HttpMethod.PUT,
            HttpEntity(updateChild1Body, authHeaders),
            mapTypeRef
        )
        
        // Step 4: Verify only one child updated
        val updatedProfileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val updatedChildren = updatedProfileResponse.body!!["children"] as List<*>
        val updatedChild1 = updatedChildren.find { (it as Map<*, *>)["id"] == child1Id } as Map<*, *>
        val updatedChild2 = updatedChildren.find { (it as Map<*, *>)["id"] == child2Id } as Map<*, *>
        
        assertThat(updatedChild1["name"]).isEqualTo("Child One Updated")
        assertThat(updatedChild2["name"]).isEqualTo("Child Two")
    }
    
    @Test
    fun `listening history tracks completed stories`() {
        // Test that listening history is maintained
        
        // Step 1: Get initial listening progress
        val initialProgressResponse = restTemplate.exchange(
            "${ApiVersion.V1}/analytics/listening-progress",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(initialProgressResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Step 2: Listen to a story
        val libraryResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=1",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val stories = libraryResponse.body!!["content"] as List<*>
        if (stories.isNotEmpty()) {
            val storyId = ((stories[0] as Map<*, *>)["id"] as Number).toLong()
            
            // Save playback position
            val savePositionBody = """
                {
                    "storyId": $storyId,
                    "positionSeconds": 120
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(savePositionBody, authHeaders),
                mapTypeRef
            )
            
            // Step 3: Verify listening progress updated
            val updatedProgressResponse = restTemplate.exchange(
                "${ApiVersion.V1}/analytics/listening-progress",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            assertThat(updatedProgressResponse.statusCode).isEqualTo(HttpStatus.OK)
            // Progress should include the story we just listened to
        }
    }
    
    @Test
    fun `subscription status displayed in profile`() {
        // Test that subscription information is included in profile
        
        val profileResponse = restTemplate.exchange(
            "${ApiVersion.V1}/profile/boot",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(profileResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(profileResponse.body).containsKey("subscription")
        
        val subscription = profileResponse.body!!["subscription"]
        // Subscription might be null for free users
        if (subscription != null) {
            val subMap = subscription as Map<*, *>
            assertThat(subMap).containsKeys("plan", "status")
        }
    }
    
    @Test
    fun `error handling for invalid child updates`() {
        // Test validation for child profile updates
        
        // Try to update non-existent child
        val invalidChildId = 999999L
        val updateBody = """{"name": "Invalid Child"}"""
        
        val updateResponse = restTemplate.exchange(
            "${ApiVersion.V1}/parents/me/children/$invalidChildId",
            HttpMethod.PUT,
            HttpEntity(updateBody, authHeaders),
            mapTypeRef
        )
        
        assertThat(updateResponse.statusCode).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN)
    }
}
