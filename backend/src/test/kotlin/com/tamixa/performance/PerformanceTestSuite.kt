package com.tamixa.performance

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.SuiteDisplayName

/**
 * Performance Test Suite
 * 
 * Runs all performance tests to verify the application meets
 * performance requirements specified in Requirements 17 and 18.
 * 
 * To run only performance tests:
 * ```
 * ./gradlew :backend:test --tests "com.tamixa.performance.PerformanceTestSuite"
 * ```
 * 
 * Or using tags:
 * ```
 * ./gradlew :backend:test -Dgroups=performance
 * ```
 * 
 * Performance Requirements Tested:
 * 
 * Mobile App Performance (Requirement 17):
 * - Initial screen render within 1 second
 * - User interaction response within 100 milliseconds
 * - Progressive image loading with placeholders
 * - Local caching for frequently accessed data
 * - Pagination for long lists
 * - Optimized image sizes
 * - Minimal memory usage
 * - Background operations don't block UI
 * 
 * Admin Dashboard Performance (Requirement 18):
 * - Story list page loads within 2 seconds
 * - Server-side pagination (20 items per page)
 * - Optimistic UI updates for bulk actions
 * - Debounced search input
 * - Pipeline status cache (3 seconds)
 * - Virtual scrolling for lists > 100 items
 * - Lazy-load images
 * - Stop polling when page not visible
 */
@Suite
@SuiteDisplayName("Performance Test Suite")
@SelectPackages("com.tamixa.performance")
@IncludeTags("performance")
class PerformanceTestSuite
