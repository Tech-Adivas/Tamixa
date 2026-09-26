# Performance Testing Guide

## Quick Start

### Run All Performance Tests
```bash
cd backend
../gradlew test --tests "com.tamixa.performance.*"
```

### Run Specific Test Suite
```bash
# API endpoint tests
../gradlew test --tests "com.tamixa.performance.ApiEndpointPerformanceTest"

# Database query tests
../gradlew test --tests "com.tamixa.performance.DatabaseQueryPerformanceTest"

# Caching tests
../gradlew test --tests "com.tamixa.performance.CachingPerformanceTest"

# Memory usage tests
../gradlew test --tests "com.tamixa.performance.MemoryUsageTest"
```

### Run with Script
```bash
chmod +x src/test/resources/performance/run-performance-tests.sh
./src/test/resources/performance/run-performance-tests.sh
```

## What Gets Tested

### 1. API Endpoint Performance
**Requirements**: 17.1, 17.2, 18.1

Tests verify:
- Home endpoint responds < 1 second (mobile dashboard)
- Library stories pagination < 2 seconds (admin dashboard)
- Story detail endpoint < 500ms (user interaction)
- Health endpoint < 100ms (monitoring)
- Concurrent requests don't degrade performance > 3x

**Key Metrics**:
- Response time (mean, P95, P99)
- Throughput (requests/second)
- Concurrent request handling

### 2. Database Query Performance
**Requirements**: 17.5, 18.2, 18.6

Tests verify:
- Pagination queries < 100ms
- Consistent performance across pages
- Translation lookups < 50ms
- Large result sets (100+ items) < 500ms
- Count queries < 50ms
- Connection acquisition < 50ms

**Key Metrics**:
- Query execution time
- Connection pool efficiency
- Pagination consistency

### 3. Caching Performance
**Requirements**: 17.4, 18.5

Tests verify:
- Cached responses faster than uncached
- Cache improves repeated request performance
- Redis operations < 50ms
- Cache expiration works (3 second requirement)
- Concurrent cache access efficient

**Key Metrics**:
- Cache hit/miss ratio
- Redis operation latency
- Cache effectiveness

### 4. Memory Usage
**Requirements**: 17.7

Tests verify:
- Pagination prevents memory exhaustion
- Large pages < 50MB memory
- No memory leaks in repeated queries
- Heap usage < 70% under load
- Efficient object allocation

**Key Metrics**:
- Memory usage (MB)
- Heap utilization (%)
- GC frequency

## Performance Thresholds

| Metric | Threshold | Requirement |
|--------|-----------|-------------|
| Home endpoint | < 1000ms | 17.1 |
| User interaction | < 100ms | 17.2 |
| Admin page load | < 2000ms | 18.1 |
| Pagination query | < 100ms | 18.2 |
| Cache duration | 3 seconds | 18.5 |
| Virtual scroll | 100 items | 18.6 |
| Memory usage | < 70% heap | 17.7 |

## Interpreting Results

### Benchmark Output Format
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

### What Each Metric Means

- **Min**: Best case performance (fastest request)
- **Max**: Worst case performance (slowest request)
- **Mean**: Average performance across all requests
- **Median**: Middle value (50% faster, 50% slower)
- **P95**: 95% of requests are faster than this
- **P99**: 99% of requests are faster than this

### Performance Evaluation

✅ **Good**: P95 < threshold
⚠️ **Warning**: P95 between threshold and 1.5x threshold
❌ **Poor**: P95 > 1.5x threshold

### Example Analysis

```
GET /api/v1/home
  P95: 850ms (threshold: 1000ms) ✅ GOOD
  
GET /api/v1/stories/library
  P95: 2500ms (threshold: 2000ms) ⚠️ WARNING
  
Database query
  P95: 250ms (threshold: 100ms) ❌ POOR - needs optimization
```

## Troubleshooting

### Tests Fail Due to Timeout
**Cause**: Environment slower than expected
**Solution**: 
- Check system resources (CPU, memory)
- Ensure no other heavy processes running
- Consider adjusting thresholds for CI environment

### Redis Tests Skipped
**Cause**: Redis not available
**Solution**:
- Tests gracefully skip if Redis unavailable
- To enable: Start Redis locally (`docker run -p 6379:6379 redis`)
- Or use Testcontainers Redis module

### Memory Tests Fail
**Cause**: Insufficient heap or memory leaks
**Solution**:
- Increase heap: Add `-Xmx2g` to test JVM args
- Check for memory leaks in application code
- Run with profiler to identify issues

### Inconsistent Results
**Cause**: JIT compilation, GC, or system load
**Solution**:
- Tests include warm-up iterations
- Run multiple times and compare
- Use dedicated test environment

## Best Practices

### Before Running Tests
1. Close unnecessary applications
2. Ensure stable network connection
3. Use consistent hardware for comparison
4. Run on same environment as production

### During Development
1. Run performance tests before committing
2. Compare results with baseline
3. Investigate any regressions
4. Document performance improvements

### In CI/CD
1. Run on every PR
2. Track performance trends over time
3. Fail build if thresholds exceeded
4. Generate performance reports

## Performance Optimization Tips

### API Endpoints
- Use caching for frequently accessed data
- Implement pagination for large datasets
- Optimize database queries (indexes, joins)
- Use connection pooling
- Enable HTTP compression

### Database Queries
- Add indexes on frequently queried columns
- Use EXPLAIN ANALYZE to identify slow queries
- Avoid N+1 queries (use JOIN or batch loading)
- Use appropriate fetch sizes
- Consider read replicas for heavy read loads

### Caching
- Cache expensive computations
- Use appropriate TTL values
- Implement cache warming for critical data
- Monitor cache hit rates
- Use Redis for distributed caching

### Memory Management
- Use pagination to limit result sets
- Avoid loading entire collections
- Close resources properly (use try-with-resources)
- Monitor for memory leaks
- Tune JVM garbage collection

## Continuous Monitoring

### Metrics to Track
- Response time trends
- Throughput (requests/second)
- Error rates
- Database query performance
- Cache hit rates
- Memory usage
- CPU utilization

### Tools
- Application Performance Monitoring (APM)
- Database query profilers
- JVM profilers (VisualVM, YourKit)
- Load testing tools (JMeter, Gatling)
- Real User Monitoring (RUM)

## Next Steps

After running performance tests:

1. **Review Results**: Check all tests passed
2. **Analyze Metrics**: Look for bottlenecks
3. **Compare Baseline**: Track performance over time
4. **Optimize**: Address any issues found
5. **Re-test**: Verify improvements
6. **Document**: Record findings and changes

## References

- [Performance Test README](../../kotlin/com/tamixa/performance/README.md)
- [Requirements Document](../../../../.kiro/specs/tamixa-premium-ux-overhaul/requirements.md)
- [Design Document](../../../../.kiro/specs/tamixa-premium-ux-overhaul/design.md)
