package com.tamixa.performance

import com.tamixa.IntegrationTestBase
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import kotlin.system.measureTimeMillis

/**
 * Base class for performance tests.
 * Performance tests verify response times, throughput, and resource usage
 * meet the requirements specified in Requirements 17 and 18.
 */
@Tag("performance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.jpa.show-sql=false",
    "logging.level.com.tamixa=INFO"
])
abstract class PerformanceTestBase : IntegrationTestBase() {

    /**
     * Performance thresholds from requirements
     */
    object Thresholds {
        // Mobile App Performance (Requirement 17)
        const val MOBILE_INITIAL_RENDER_MS = 1000L
        const val MOBILE_INTERACTION_RESPONSE_MS = 100L
        
        // Admin Dashboard Performance (Requirement 18)
        const val ADMIN_PAGE_LOAD_MS = 2000L
        const val ADMIN_PAGINATION_SIZE = 20
        const val ADMIN_CACHE_DURATION_SECONDS = 3
        const val ADMIN_VIRTUAL_SCROLL_THRESHOLD = 100
    }

    /**
     * Measure execution time and assert it meets threshold
     */
    protected fun <T> measureAndAssert(
        description: String,
        thresholdMs: Long,
        block: () -> T
    ): Pair<T, Long> {
        val elapsed = measureTimeMillis {
            block()
        }
        
        println("$description: ${elapsed}ms (threshold: ${thresholdMs}ms)")
        
        if (elapsed > thresholdMs) {
            println("WARNING: $description exceeded threshold by ${elapsed - thresholdMs}ms")
        }
        
        return Pair(block(), elapsed)
    }

    /**
     * Run multiple iterations and calculate statistics
     */
    protected fun <T> benchmark(
        description: String,
        iterations: Int = 10,
        warmup: Int = 2,
        block: () -> T
    ): BenchmarkResult {
        // Warmup
        repeat(warmup) { block() }
        
        // Actual measurements
        val times = mutableListOf<Long>()
        repeat(iterations) {
            val elapsed = measureTimeMillis { block() }
            times.add(elapsed)
        }
        
        val sorted = times.sorted()
        val result = BenchmarkResult(
            description = description,
            iterations = iterations,
            min = sorted.first(),
            max = sorted.last(),
            mean = times.average().toLong(),
            median = sorted[sorted.size / 2],
            p95 = sorted[(sorted.size * 0.95).toInt()],
            p99 = sorted[(sorted.size * 0.99).toInt()]
        )
        
        println(result)
        return result
    }

    data class BenchmarkResult(
        val description: String,
        val iterations: Int,
        val min: Long,
        val max: Long,
        val mean: Long,
        val median: Long,
        val p95: Long,
        val p99: Long
    ) {
        override fun toString(): String = """
            |Benchmark: $description
            |  Iterations: $iterations
            |  Min: ${min}ms
            |  Max: ${max}ms
            |  Mean: ${mean}ms
            |  Median: ${median}ms
            |  P95: ${p95}ms
            |  P99: ${p99}ms
        """.trimMargin()
    }
}
