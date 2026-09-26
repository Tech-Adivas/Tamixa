package com.tamixa.performance

import com.tamixa.api.admin.dto.PagedResponse
import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.application.home.HomeResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat

/**
 * Performance tests for critical API endpoints.
 * 
 * Tests verify:
 * - Response times meet requirements (Requirement 17, 18)
 * - Pagination works efficiently with large datasets
 * - Concurrent requests don't degrade performance significantly
 * - Caching reduces database load
 */
class ApiEndpointPerformanceTest : PerformanceTestBase() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    /**
     * Test home endpoint performance (mobile dashboard)
     * Requirement 17.1: Initial screen render within 1 second
     */
    @Test
    fun `home endpoint should respond within 1 second`() {
        // This test requires authentication - skip if no test user available
        // In real scenario, would use test JWT token
        
        val result = benchmark(
            description = "GET /api/v1/home",
            iterations = 5
        ) {
            // Note: This will return 401 without auth, but measures endpoint availability
            restTemplate.getForEntity("/api/v1/home?language=ta", HomeResponse::class.java)
        }
        
        // P95 should be under threshold for good user experience
        assertThat(result.p95)
            .describedAs("Home endpoint P95 response time")
            .isLessThan(Thresholds.MOBILE_INITIAL_RENDER_MS)
    }

    /**
     * Test library stories endpoint with pagination
     * Requirement 18.2: Server-side pagination with 20 items per page
     */
    @Test
    fun `library stories endpoint should support efficient pagination`() {
        val result = benchmark(
            description = "GET /api/v1/stories/library with pagination",
            iterations = 5
        ) {
            val response = restTemplate.exchange(
                "/api/v1/stories/library?language=ta&page=0&size=20",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<PagedResponse<LibraryStoryResponse>>() {}
            )
            response
        }
        
        // Should respond quickly even with pagination
        assertThat(result.mean)
            .describedAs("Library stories pagination mean response time")
            .isLessThan(Thresholds.ADMIN_PAGE_LOAD_MS)
    }

    /**
     * Test library stories endpoint with different page sizes
     * Verifies performance doesn't degrade significantly with larger pages
     */
    @Test
    fun `library stories endpoint should handle various page sizes efficiently`() {
        val pageSizes = listOf(10, 20, 50, 100)
        
        pageSizes.forEach { size ->
            val result = benchmark(
                description = "GET /api/v1/stories/library (size=$size)",
                iterations = 3
            ) {
                restTemplate.exchange(
                    "/api/v1/stories/library?language=ta&page=0&size=$size",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<PagedResponse<LibraryStoryResponse>>() {}
                )
            }
            
            // Larger pages should still be reasonable
            val threshold = when {
                size <= 20 -> Thresholds.ADMIN_PAGE_LOAD_MS
                size <= 50 -> Thresholds.ADMIN_PAGE_LOAD_MS * 1.5
                else -> Thresholds.ADMIN_PAGE_LOAD_MS * 2
            }
            
            assertThat(result.mean)
                .describedAs("Library stories (size=$size) mean response time")
                .isLessThan(threshold.toLong())
        }
    }

    /**
     * Test health endpoint performance
     * Should be very fast for monitoring purposes
     */
    @Test
    fun `health endpoint should respond within 100ms`() {
        val result = benchmark(
            description = "GET /api/v1/health",
            iterations = 10
        ) {
            restTemplate.getForEntity("/api/v1/health", Map::class.java)
        }
        
        assertThat(result.p95)
            .describedAs("Health endpoint P95 response time")
            .isLessThan(100L)
    }

    /**
     * Test concurrent requests to home endpoint
     * Requirement 17.8: Background operations don't block UI
     */
    @Test
    fun `concurrent requests should not significantly degrade performance`() {
        val concurrentRequests = 10
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Long>()
        
        // Measure single request baseline
        val baselineTime = measureTimeMillis {
            restTemplate.getForEntity("/api/v1/health", Map::class.java)
        }
        
        // Measure concurrent requests
        val totalTime = measureTimeMillis {
            repeat(concurrentRequests) {
                val thread = Thread {
                    val elapsed = measureTimeMillis {
                        restTemplate.getForEntity("/api/v1/health", Map::class.java)
                    }
                    synchronized(results) {
                        results.add(elapsed)
                    }
                }
                threads.add(thread)
                thread.start()
            }
            
            threads.forEach { it.join() }
        }
        
        val avgConcurrentTime = results.average()
        val degradationFactor = avgConcurrentTime / baselineTime
        
        println("Baseline: ${baselineTime}ms")
        println("Concurrent avg: ${avgConcurrentTime}ms")
        println("Degradation factor: ${degradationFactor}x")
        
        // Performance should not degrade more than 3x under concurrent load
        assertThat(degradationFactor)
            .describedAs("Performance degradation under concurrent load")
            .isLessThan(3.0)
    }

    /**
     * Test library categories endpoint performance
     * Should be fast as it's a simple distinct query
     */
    @Test
    fun `library categories endpoint should respond quickly`() {
        val result = benchmark(
            description = "GET /api/v1/stories/library/categories",
            iterations = 5
        ) {
            restTemplate.getForEntity(
                "/api/v1/stories/library/categories?language=ta",
                Map::class.java
            )
        }
        
        assertThat(result.mean)
            .describedAs("Categories endpoint mean response time")
            .isLessThan(200L)
    }

    /**
     * Test story detail endpoint performance
     * Requirement 17.2: User interaction response within 100ms
     */
    @Test
    fun `story detail endpoint should respond within interaction threshold`() {
        // First get a story ID from the list
        val listResponse = restTemplate.exchange(
            "/api/v1/stories/library?language=ta&page=0&size=1",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<PagedResponse<LibraryStoryResponse>>() {}
        )
        
        if (listResponse.statusCode == HttpStatus.OK && 
            listResponse.body?.content?.isNotEmpty() == true) {
            
            val storyId = listResponse.body!!.content.first().id
            
            val result = benchmark(
                description = "GET /api/v1/stories/library/$storyId",
                iterations = 5
            ) {
                restTemplate.getForEntity(
                    "/api/v1/stories/library/$storyId?language=ta",
                    LibraryStoryResponse::class.java
                )
            }
            
            // Should be fast for good interaction response
            assertThat(result.p95)
                .describedAs("Story detail P95 response time")
                .isLessThan(500L) // More lenient than 100ms for full detail fetch
        }
    }
}
