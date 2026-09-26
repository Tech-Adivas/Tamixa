package com.tamixa.performance

import com.tamixa.infrastructure.persistence.LibraryStoryJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.assertj.core.api.Assertions.assertThat

/**
 * Memory usage and resource optimization tests.
 * 
 * Tests verify:
 * - Memory usage stays within acceptable limits (Requirement 17.7)
 * - Large result sets don't cause memory issues
 * - Pagination prevents memory exhaustion
 */
class MemoryUsageTest : PerformanceTestBase() {

    @Autowired
    private lateinit var libraryStoryRepository: LibraryStoryJpaRepository

    /**
     * Test memory usage with pagination
     * Requirement 17.5: Pagination for long lists
     */
    @Test
    fun `pagination should prevent excessive memory usage`() {
        val runtime = Runtime.getRuntime()
        
        // Force garbage collection to get baseline
        System.gc()
        Thread.sleep(100)
        
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Load multiple pages
        val pages = 10
        val pageSize = 20
        
        repeat(pages) { pageNum ->
            libraryStoryRepository.findAll(PageRequest.of(pageNum, pageSize))
        }
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024) // Convert to MB
        
        println("Memory before: ${memoryBefore / (1024 * 1024)}MB")
        println("Memory after: ${memoryAfter / (1024 * 1024)}MB")
        println("Memory used: ${memoryUsed}MB")
        
        // Memory usage should be reasonable (less than 100MB for this operation)
        assertThat(memoryUsed)
            .describedAs("Memory used for paginated queries")
            .isLessThan(100L)
    }

    /**
     * Test memory usage with large page sizes
     * Verifies that even large pages don't cause excessive memory usage
     */
    @Test
    fun `large page sizes should not cause memory exhaustion`() {
        val runtime = Runtime.getRuntime()
        
        System.gc()
        Thread.sleep(100)
        
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Load a large page
        val largePageSize = 100
        libraryStoryRepository.findAll(PageRequest.of(0, largePageSize))
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024)
        
        println("Large page (size=$largePageSize) memory used: ${memoryUsed}MB")
        
        // Should still be reasonable
        assertThat(memoryUsed)
            .describedAs("Memory used for large page")
            .isLessThan(50L)
    }

    /**
     * Test memory usage with repeated queries
     * Verifies no memory leaks in query execution
     */
    @Test
    fun `repeated queries should not leak memory`() {
        val runtime = Runtime.getRuntime()
        
        System.gc()
        Thread.sleep(100)
        
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Execute many queries
        repeat(100) {
            libraryStoryRepository.findAll(PageRequest.of(0, 10))
        }
        
        System.gc()
        Thread.sleep(100)
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryGrowth = (memoryAfter - memoryBefore) / (1024 * 1024)
        
        println("Memory growth after 100 queries: ${memoryGrowth}MB")
        
        // Memory growth should be minimal (less than 50MB)
        assertThat(memoryGrowth)
            .describedAs("Memory growth after repeated queries")
            .isLessThan(50L)
    }

    /**
     * Test heap usage under load
     * Verifies application doesn't approach heap limits
     */
    @Test
    fun `heap usage should remain healthy under load`() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        
        // Simulate load
        repeat(50) {
            libraryStoryRepository.findAll(PageRequest.of(0, 20))
        }
        
        System.gc()
        Thread.sleep(100)
        
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val heapUsagePercent = (usedMemory.toDouble() / maxMemory) * 100
        
        println("Max heap: ${maxMemory / (1024 * 1024)}MB")
        println("Used heap: ${usedMemory / (1024 * 1024)}MB")
        println("Heap usage: ${heapUsagePercent}%")
        
        // Should not use more than 70% of heap under normal load
        assertThat(heapUsagePercent)
            .describedAs("Heap usage percentage")
            .isLessThan(70.0)
    }

    /**
     * Test object allocation rate
     * Verifies efficient object creation and garbage collection
     */
    @Test
    fun `object allocation should be efficient`() {
        val runtime = Runtime.getRuntime()
        
        // Measure allocations during query execution
        System.gc()
        Thread.sleep(100)
        
        val gcCountBefore = getGcCount()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Execute queries
        repeat(20) {
            libraryStoryRepository.findAll(PageRequest.of(0, 20))
        }
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val gcCountAfter = getGcCount()
        
        val gcTriggered = gcCountAfter - gcCountBefore
        val memoryAllocated = (memoryAfter - memoryBefore) / (1024 * 1024)
        
        println("GC collections triggered: $gcTriggered")
        println("Memory allocated: ${memoryAllocated}MB")
        
        // Should not trigger excessive GC
        assertThat(gcTriggered)
            .describedAs("GC collections during queries")
            .isLessThan(10L)
    }

    private fun getGcCount(): Long {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
            .sumOf { it.collectionCount }
    }
}
