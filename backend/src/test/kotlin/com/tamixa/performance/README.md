# Performance Testing

This directory contains performance tests for the Tamixa Premium UX Overhaul, verifying that the application meets the performance requirements specified in Requirements 17 and 18.

## Overview

Performance testing ensures:
- **Mobile App Performance** (Requirement 17): Fast initial render, responsive interactions, efficient caching
- **Admin Dashboard Performance** (Requirement 18): Quick page loads, efficient pagination, optimized queries

## Test Structure

### PerformanceTestBase
Base class providing:
- Performance thresholds from requirements
- Benchmarking utilities
- Statistical analysis (min, max, mean, median, P95, P99)

### Test Suites

#### 1. ApiEndpointPerformanceTest
Tests critical API endpoint performance:
- Home endpoint (mobile dashboard) - < 1 second
- Library stories with pagination - < 2 seconds
- Story detail endpoint - < 500ms
- Concurrent request handling
- Health endpoint - < 100ms

#### 2. DatabaseQueryPerformanceTest
Tests database query efficiency:
- Pagination query performance
- Consistent performance across pages
- Translation lookup speed
- Large result set handling
- Count query optimization
- Connection pool performance
- Concurrent query handling

#### 3. CachingPerformanceTest
Tests caching mechanisms:
- Cache hit vs miss performance
- Cache performance under load
- Redis operation speed
- Cache expiration (3 second requirement)
- Concurrent cache access

#### 4. MemoryUsageTest
Tests memory optimization:
- Pagination prevents memory exhaustion
- Large page size handling
- Memory leak detection
- Heap usage under load
- Object allocation efficiency

## Running Tests

### Run All Performance Tests
```bash
./gradlew :backend:test --tests "com.tamixa.performance.*"
```

### Run Specific Test Suite
```bash
./gradlew :backend:test --tests "com.tamixa.performance.ApiEndpointPerformanceTest"
```

### Run with Performance Tag
```bash
./gradlew :backend:test -Dgroups=performance
```

### Run Performance Test Suite
```bash
./gradlew :backend:test --tests "com.tamixa.performance.PerformanceTestSuite"
```

## Performance Thresholds

### Mobile App (Requirement 17)
| Metric | Threshold | Test |
|--------|-----------|------|
| Initial screen render | < 1000ms | ApiEndpointPerformanceTest |
| User interaction response | < 100ms | ApiEndpointPerformanceTest |
| Cached data access | Fast | CachingPerformanceTest |
| Memory usage | Minimal | MemoryUsageTest |

### Admin Dashboard (Requirement 18)
| Metric | Threshold | Test |
|--------|-----------|------|
| Page load | < 2000ms | ApiEndpointPerformanceTest |
| Pagination size | 20 items | DatabaseQueryPerformanceTest |
| Cache duration | 3 seconds | CachingPerformanceTest |
| Virtual scroll threshold | 100 items | DatabaseQueryPerformanceTest |

## Interpreting Results

### Benchmark Output
Each benchmark provides:
- **Min**: Fastest execution time
- **Max**: Slowest execution time
- **Mean**: Average execution time
- **Median**: Middle value (50th percentile)
- **P95**: 95th percentile (95% of requests faster)
- **P99**: 99th percentile (99% of requests faster)

### Example Output
```
Benchmark: GET /api/v1/home
  Iterations: 10
  Min: 45ms
  Max: 120ms
  Mean: 67ms
  Median: 65ms
  P95: 110ms
  P99: 120ms
```

### Performance Degradation
Tests measure performance degradation under load:
- **Degradation Factor**: Concurrent time / Baseline time
- **Acceptable**: < 3x degradation under concurrent load
- **Warning**: > 3x indicates potential bottleneck

## Test Environment

### Requirements
- PostgreSQL (via Testcontainers)
- Redis (optional, tests skip if unavailable)
- Sufficient heap memory (tests verify < 70% usage)

### Configuration
Tests use `application-test.yml` with:
- Reduced logging (INFO level)
- SQL logging disabled
- Test-specific connection pool settings

## Continuous Integration

Performance tests should be run:
- **Pre-merge**: On all PRs to catch regressions
- **Nightly**: Full suite with detailed reporting
- **Release**: Before each release to verify performance

### CI Configuration
```yaml
# Example GitHub Actions
- name: Run Performance Tests
  run: ./gradlew :backend:test --tests "com.tamixa.performance.*"
  
- name: Check Performance Thresholds
  run: |
    # Parse test results and fail if thresholds exceeded
    # (Implementation depends on CI system)
```

## Troubleshooting

### Tests Failing
1. **Check thresholds**: May need adjustment for CI environment
2. **Database performance**: Ensure Testcontainers has sufficient resources
3. **Network latency**: Local vs CI environment differences
4. **Concurrent tests**: May need to adjust `maxParallelForks` in build.gradle

### Redis Tests Skipped
- Redis is optional in test environment
- Tests gracefully skip if Redis unavailable
- To enable: Start Redis locally or use Testcontainers Redis

### Memory Tests Failing
- Increase heap size: `-Xmx2g` in test JVM args
- Check for memory leaks in application code
- Verify garbage collection is working

## Best Practices

1. **Run locally first**: Verify tests pass before CI
2. **Baseline measurements**: Establish baseline on target hardware
3. **Consistent environment**: Use same resources for comparison
4. **Warm-up iterations**: Tests include warm-up to stabilize JIT
5. **Statistical significance**: Multiple iterations for reliable results

## Future Enhancements

- [ ] Load testing with JMeter or Gatling
- [ ] Stress testing to find breaking points
- [ ] Endurance testing for memory leaks
- [ ] Real user monitoring (RUM) integration
- [ ] Performance regression tracking
- [ ] Automated performance reports
- [ ] Database query profiling
- [ ] APM integration (New Relic, DataDog)

## References

- [Requirements Document](../../../.kiro/specs/tamixa-premium-ux-overhaul/requirements.md)
- [Design Document](../../../.kiro/specs/tamixa-premium-ux-overhaul/design.md)
- [Task 22: Performance Testing](../../../.kiro/specs/tamixa-premium-ux-overhaul/tasks.md)
