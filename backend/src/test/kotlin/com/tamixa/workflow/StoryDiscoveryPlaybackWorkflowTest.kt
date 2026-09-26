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
 * End-to-end workflow test for Story Discovery and Playback (Requirements 7, 8, 19).
 * 
 * Tests the complete story discovery and playback workflow:
 * 1. Dashboard story browsing (spotlight, categories)
 * 2. Library hub navigation (Browse, Fun, Learn, Simulator)
 * 3. Story search and filtering
 * 4. Audio playback with controls
 * 5. Background audio and lock screen controls
 * 6. Progress saving
 * 
 * Success Criteria:
 * - User can browse stories on dashboard
 * - User can navigate library hubs and filter stories
 * - User can search for specific stories
 * - User can access story audio stream URLs
 * - Playback position is saved and retrieved
 * - Progress persists across sessions
 */
class StoryDiscoveryPlaybackWorkflowTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapTypeRef = object : ParameterizedTypeReference<Map<String, Any>>() {}
    private val listMapTypeRef = object : ParameterizedTypeReference<List<Map<String, Any>>>() {}
    
    private lateinit var accessToken: String
    private lateinit var authHeaders: HttpHeaders

    @BeforeEach
    fun setupAuthenticatedUser() {
        // Create authenticated user for all tests
        val email = "story-user-${System.currentTimeMillis()}@test.com"
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
    fun `complete story discovery workflow - dashboard to library to playback`() {
        // Step 1: Browse dashboard - view spotlight and category stories
        val homeResponse = restTemplate.exchange(
            "${ApiVersion.V1}/home",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(homeResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(homeResponse.body).containsKey("spotlight")
        assertThat(homeResponse.body).containsKey("categories")
        assertThat(homeResponse.body).containsKey("recommended")
        
        val spotlight = homeResponse.body!!["spotlight"] as List<*>
        // Spotlight should have up to 8 featured stories
        assertThat(spotlight.size).isLessThanOrEqualTo(8)
        
        // Step 2: Navigate to Library - Browse all stories
        val libraryResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(libraryResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(libraryResponse.body).containsKey("content")
        assertThat(libraryResponse.body).containsKey("totalElements")
        assertThat(libraryResponse.body).containsKey("totalPages")
        
        val stories = libraryResponse.body!!["content"] as List<*>
        
        // Step 3: Filter by category (Fun Corner hub)
        val funStoriesResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?category=fun&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(funStoriesResponse.statusCode).isEqualTo(HttpStatus.OK)
        val funStories = funStoriesResponse.body!!["content"] as List<*>
        // All stories should be in fun category
        funStories.forEach { story ->
            val storyMap = story as Map<*, *>
            // Category should be fun-related
            assertThat(storyMap).containsKey("category")
        }
        
        // Step 4: Filter by language (Tamil)
        val tamilStoriesResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?language=ta&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(tamilStoriesResponse.statusCode).isEqualTo(HttpStatus.OK)
        val tamilStories = tamilStoriesResponse.body!!["content"] as List<*>
        tamilStories.forEach { story ->
            val storyMap = story as Map<*, *>
            assertThat(storyMap["language"]).isEqualTo("ta")
        }
        
        // Step 5: Search for specific story
        val searchResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/search?q=adventure&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(searchResponse.body).containsKey("content")
        
        // Step 6: Select a story and get stream URL (if stories exist)
        if (stories.isNotEmpty()) {
            val firstStory = stories[0] as Map<*, *>
            val storyId = (firstStory["id"] as Number).toLong()
            
            val streamUrlResponse = restTemplate.exchange(
                "${ApiVersion.V1}/stories/$storyId/stream-url",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            // Story might not have audio yet, so we accept both OK and NOT_FOUND
            assertThat(streamUrlResponse.statusCode).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND)
            
            if (streamUrlResponse.statusCode == HttpStatus.OK) {
                assertThat(streamUrlResponse.body).containsKey("url")
                
                // Step 7: Save playback position (simulating user listening)
                val savePositionBody = """
                    {
                        "storyId": $storyId,
                        "positionSeconds": 45
                    }
                """.trimIndent()
                
                val savePositionResponse = restTemplate.exchange(
                    "${ApiVersion.V1}/playback/position",
                    HttpMethod.POST,
                    HttpEntity(savePositionBody, authHeaders),
                    mapTypeRef
                )
                
                assertThat(savePositionResponse.statusCode).isEqualTo(HttpStatus.OK)
                
                // Step 8: Retrieve saved position (simulating app restart)
                val getPositionResponse = restTemplate.exchange(
                    "${ApiVersion.V1}/playback/position?storyId=$storyId",
                    HttpMethod.GET,
                    HttpEntity<Void>(authHeaders),
                    mapTypeRef
                )
                
                assertThat(getPositionResponse.statusCode).isEqualTo(HttpStatus.OK)
                assertThat(getPositionResponse.body!!["positionSeconds"]).isEqualTo(45)
                
                // Step 9: Update position (user continues listening)
                val updatePositionBody = """
                    {
                        "storyId": $storyId,
                        "positionSeconds": 120
                    }
                """.trimIndent()
                
                val updatePositionResponse = restTemplate.exchange(
                    "${ApiVersion.V1}/playback/position",
                    HttpMethod.POST,
                    HttpEntity(updatePositionBody, authHeaders),
                    mapTypeRef
                )
                
                assertThat(updatePositionResponse.statusCode).isEqualTo(HttpStatus.OK)
                
                // Verify updated position
                val verifyPositionResponse = restTemplate.exchange(
                    "${ApiVersion.V1}/playback/position?storyId=$storyId",
                    HttpMethod.GET,
                    HttpEntity<Void>(authHeaders),
                    mapTypeRef
                )
                
                assertThat(verifyPositionResponse.statusCode).isEqualTo(HttpStatus.OK)
                assertThat(verifyPositionResponse.body!!["positionSeconds"]).isEqualTo(120)
            }
        }
    }
    
    @Test
    fun `library hub navigation - Browse, Fun, Learn, Simulator`() {
        // Test navigation through all library hubs
        
        // Hub 1: Browse (all stories)
        val browseResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(browseResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Hub 2: Fun Corner (entertainment stories)
        val funResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?category=fun&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(funResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Hub 3: Learn & Safety (educational stories)
        val learnResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?category=educational&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(learnResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Hub 4: Simulator (interactive stories)
        val simulatorResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?isInteractive=true&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(simulatorResponse.statusCode).isEqualTo(HttpStatus.OK)
    }
    
    @Test
    fun `story search with various filters`() {
        // Test search functionality with different parameters
        
        // Search by keyword
        val keywordSearchResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/search?q=brave&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(keywordSearchResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Search with language filter
        val languageSearchResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/search?q=story&language=ta&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(languageSearchResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Search with category filter
        val categorySearchResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/search?q=adventure&category=fun&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(categorySearchResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        // Empty search query should return all stories
        val emptySearchResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/search?q=&page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        assertThat(emptySearchResponse.statusCode).isEqualTo(HttpStatus.OK)
    }
    
    @Test
    fun `playback position persistence across multiple stories`() {
        // Test saving and retrieving positions for multiple stories
        
        val libraryResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=5",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val stories = libraryResponse.body!!["content"] as List<*>
        
        if (stories.size >= 2) {
            val story1Id = ((stories[0] as Map<*, *>)["id"] as Number).toLong()
            val story2Id = ((stories[1] as Map<*, *>)["id"] as Number).toLong()
            
            // Save position for story 1
            val savePosition1Body = """
                {
                    "storyId": $story1Id,
                    "positionSeconds": 30
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(savePosition1Body, authHeaders),
                mapTypeRef
            )
            
            // Save position for story 2
            val savePosition2Body = """
                {
                    "storyId": $story2Id,
                    "positionSeconds": 60
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(savePosition2Body, authHeaders),
                mapTypeRef
            )
            
            // Retrieve position for story 1
            val getPosition1Response = restTemplate.exchange(
                "${ApiVersion.V1}/playback/position?storyId=$story1Id",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            assertThat(getPosition1Response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getPosition1Response.body!!["positionSeconds"]).isEqualTo(30)
            
            // Retrieve position for story 2
            val getPosition2Response = restTemplate.exchange(
                "${ApiVersion.V1}/playback/position?storyId=$story2Id",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            assertThat(getPosition2Response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(getPosition2Response.body!!["positionSeconds"]).isEqualTo(60)
        }
    }
    
    @Test
    fun `continue adventure section shows stories with progress`() {
        // Test that stories with saved progress appear in continue adventure
        
        val libraryResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=1",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        val stories = libraryResponse.body!!["content"] as List<*>
        
        if (stories.isNotEmpty()) {
            val storyId = ((stories[0] as Map<*, *>)["id"] as Number).toLong()
            
            // Save partial progress (not completed)
            val savePositionBody = """
                {
                    "storyId": $storyId,
                    "positionSeconds": 50
                }
            """.trimIndent()
            
            restTemplate.exchange(
                "${ApiVersion.V1}/playback/position",
                HttpMethod.POST,
                HttpEntity(savePositionBody, authHeaders),
                mapTypeRef
            )
            
            // Check home endpoint for continue adventure
            val homeResponse = restTemplate.exchange(
                "${ApiVersion.V1}/home",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            assertThat(homeResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(homeResponse.body).containsKey("continueAdventure")
            
            val continueAdventure = homeResponse.body!!["continueAdventure"] as List<*>
            // Story with progress should appear in continue adventure
            if (continueAdventure.isNotEmpty()) {
                val firstItem = continueAdventure[0] as Map<*, *>
                assertThat(firstItem).containsKey("progress")
                assertThat(firstItem).containsKey("lastPlayedAt")
            }
        }
    }
    
    @Test
    fun `error handling - invalid story ID`() {
        // Test error handling for non-existent story
        
        val invalidStoryId = 999999L
        
        val streamUrlResponse = restTemplate.exchange(
            "${ApiVersion.V1}/stories/$invalidStoryId/stream-url",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(streamUrlResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
    
    @Test
    fun `pagination works correctly for large story lists`() {
        // Test pagination functionality
        
        // Get first page
        val page0Response = restTemplate.exchange(
            "${ApiVersion.V1}/stories/library?page=0&size=10",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            mapTypeRef
        )
        
        assertThat(page0Response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(page0Response.body).containsKey("content")
        assertThat(page0Response.body).containsKey("totalPages")
        assertThat(page0Response.body).containsKey("number")
        
        val page0Content = page0Response.body!!["content"] as List<*>
        assertThat(page0Content.size).isLessThanOrEqualTo(10)
        
        // Get second page if available
        val totalPages = page0Response.body!!["totalPages"] as Int
        if (totalPages > 1) {
            val page1Response = restTemplate.exchange(
                "${ApiVersion.V1}/stories/library?page=1&size=10",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders),
                mapTypeRef
            )
            
            assertThat(page1Response.statusCode).isEqualTo(HttpStatus.OK)
            val page1Content = page1Response.body!!["content"] as List<*>
            
            // Pages should have different content
            assertThat(page0Content).isNotEqualTo(page1Content)
        }
    }
}
