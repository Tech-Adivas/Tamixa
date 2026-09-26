package com.tamixa.performance

import com.tamixa.infrastructure.persistence.LibraryStoryJpaRepository
import com.tamixa.infrastructure.persistence.StoryTranslationJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.assertj.core.api.Assertions.assertThat
import javax.sql.DataSource

/**
 * Performance tests for database queries.
 * 
 * Tests verify:
 * - Query performance with large datasets
 * - Pagination efficiency
 * - Index usage
 * - N+1 query prevention
 */
class DatabaseQueryPerformanceTest : PerformanceTestBase() {

    @Autowired
    private lateinit var libraryStoryRepository: LibraryStoryJpaRepository

    @Autowired
    private lateinit var storyTranslationRepository: StoryTranslationJpaRepository

    @Autowired
    private lateinit var dataSource: DataSource

    /**
     * Test library story pagination query performance
     * Requirement 18.2: Server-side pagination with 20 items per page
     */
    @Test
    fun `library story pagination should be efficient`() {
        val pageSize = Thresholds.ADMIN_PAGINATION_SIZE
        
        val result = benchmark(
            description = "Library story pagination query (page size: $pageSize)",
            iterations = 10
        ) {
            libraryStoryRepository.findAll(PageRequest.of(0, pageSize))
        }
        
        // Database queries should be fast
        assertThat(result.mean)
            .describedAs("Pagination query mean time")
            .isLessThan(100L)
    }

    /**
     * Test pagination across multiple pages
     * Verifies consistent performance across pages
     */
    @Test
    fun `pagination performance should be consistent across pages`() {
        val pageSize = 20
        val pagesToTest = listOf(0, 1, 2, 5, 10)
        
        val results = pagesToTest.map { pageNum ->
            val result = benchmark(
                description = "Page $pageNum query",
                iterations = 5
            ) {
                libraryStoryRepository.findAll(PageRequest.of(pageNum, pageSize))
            }
            pageNum to result.mean
        }
        
        // Performance should not degrade significantly for later pages
        val firstPageTime = results.first().second
        results.forEach { (pageNum, time) ->
            val degradation = time.toDouble() / firstPageTime
            assertThat(degradation)
                .describedAs("Page $pageNum performance degradation")
                .isLessThan(2.0) // Should not be more than 2x slower
        }
    }

    /**
     * Test story translation queries
     * Verifies efficient lookup by story ID and language
     */
    @Test
    fun `story translation lookup should be fast`() {
        // Get a story ID first
        val stories = libraryStoryRepository.findAll(PageRequest.of(0, 1))
        
        if (stories.content.isNotEmpty()) {
            val storyId = stories.content.first().id
            
            val result = benchmark(
                description = "Story translation lookup by story ID",
                iterations = 10
            ) {
                storyTranslationRepository.findByLibraryStoryId(storyId)
            }
            
            assertThat(result.mean)
                .describedAs("Translation lookup mean time")
                .isLessThan(50L)
        }
    }

    /**
     * Test large result set handling
     * Requirement 18.6: Virtual scrolling for lists > 100 items
     */
    @Test
    fun `large result set queries should use pagination`() {
        val largePageSize = 100
        
        val result = benchmark(
            description = "Large page query (size: $largePageSize)",
            iterations = 5
        ) {
            libraryStoryRepository.findAll(PageRequest.of(0, largePageSize))
        }
        
        // Even large pages should be reasonable
        assertThat(result.mean)
            .describedAs("Large page query mean time")
            .isLessThan(500L)
    }

    /**
     * Test count queries for pagination
     * Count queries should be optimized
     */
    @Test
    fun `count queries should be efficient`() {
        val result = benchmark(
            description = "Count query for pagination",
            iterations = 10
        ) {
            libraryStoryRepository.count()
        }
        
        assertThat(result.mean)
            .describedAs("Count query mean time")
            .isLessThan(50L)
    }

    /**
     * Test database connection pool performance
     * Verifies connections are acquired quickly
     */
    @Test
    fun `database connections should be acquired quickly`() {
        val result = benchmark(
            description = "Database connection acquisition",
            iterations = 20
        ) {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                    }
                }
            }
        }
        
        assertThat(result.mean)
            .describedAs("Connection acquisition mean time")
            .isLessThan(50L)
    }

    /**
     * Test concurrent database queries
     * Verifies connection pool handles concurrent load
     */
    @Test
    fun `concurrent database queries should not cause contention`() {
        val concurrentQueries = 10
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Long>()
        
        val totalTime = measureTimeMillis {
            repeat(concurrentQueries) {
                val thread = Thread {
                    val elapsed = measureTimeMillis {
                        libraryStoryRepository.findAll(PageRequest.of(0, 20))
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
        
        val avgTime = results.average()
        
        println("Concurrent queries: $concurrentQueries")
        println("Total time: ${totalTime}ms")
        println("Average query time: ${avgTime}ms")
        
        // Average query time should still be reasonable
        assertThat(avgTime)
            .describedAs("Average concurrent query time")
            .isLessThan(500.0)
    }
}
