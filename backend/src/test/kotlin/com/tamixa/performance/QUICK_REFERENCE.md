# Performance Testing Quick Reference

## Run Tests

```bash
# All performance tests
./gradlew :backend:test --tests "com.tamixa.performance.*"

# Specific suite
./gradlew :backend:test --tests "com.tamixa.performance.ApiEndpointPerformanceTest"

# With tag
./gradlew :backend:test -Dgroups=performance
```

## Performance Thresholds

| Endpoint/Operation | Threshold | Requirement |
|-------------------|-----------|-------------|
| Home endpoint | < 1000ms | 17.1 |
| User interaction | < 100ms | 17.2 |
| Admin page load | < 2000ms | 18.1 |
| Pagination query | < 100ms | 18.2 |
| Cache operation | < 50ms | 17.4 |
| Memory usage | < 70% heap | 17.7 |

## Test Suites

### ApiEndpointPerformanceTest
- Home endpoint performance
- Library stories pagination
- Story detail endpoint
- Concurrent request handling
- Health endpoint

### DatabaseQueryPerformanceTest
- Pagination efficiency
- Query consistency across pages
- Translation lookups
- Large result sets
- Connection pool

### CachingPerformanceTest
- Cache hit/miss performance
- Redis operations
- Cache expiration
- Concurrent cache access

### MemoryUsageTest
- Pagination memory usage
- Memory leak detection
- Heap usage under load
- GC frequency

## Interpreting Results

```
Benchmark: GET /api/v1/home
  Min: 45ms      ← Best case
  Max: 120ms     ← Worst case
  Mean: 67ms     ← Average
  Median: 65ms   ← Middle value
  P95: 110ms     ← 95% faster than this ⭐
  P99: 120ms     ← 99% faster than this
```

**Key Metric**: P95 should be < threshold

## Status Indicators

- ✅ **Good**: P95 < threshold
- ⚠️ **Warning**: P95 between threshold and 1.5x
- ❌ **Poor**: P95 > 1.5x threshold

## Common Issues

### Redis Tests Skipped
```bash
# Start Redis
docker run -p 6379:6379 redis
```

### Memory Tests Fail
```bash
# Increase heap
./gradlew test -Dorg.gradle.jvmargs="-Xmx2g"
```

### Inconsistent Results
- Run multiple times
- Close other applications
- Use dedicated test environment

## Quick Optimization Checklist

- [ ] Add database indexes
- [ ] Enable caching
- [ ] Use pagination
- [ ] Optimize queries (EXPLAIN ANALYZE)
- [ ] Enable connection pooling
- [ ] Monitor memory usage
- [ ] Profile slow endpoints

## Documentation

- [Full README](README.md)
- [Test Guide](../../../resources/performance/PERFORMANCE_TEST_GUIDE.md)
- [Implementation Summary](../../../../.kiro/specs/tamixa-premium-ux-overhaul/TASK_22_IMPLEMENTATION_SUMMARY.md)
