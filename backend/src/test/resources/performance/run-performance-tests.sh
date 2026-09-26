#!/usr/bin/env bash
# Performance Test Runner
# Runs all performance tests and generates a summary report

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

echo "========================================="
echo "Tamixa Performance Test Suite"
echo "========================================="
echo ""
echo "Project root: $PROJECT_ROOT"
echo "Running performance tests..."
echo ""

cd "$PROJECT_ROOT"

# Run performance tests
./gradlew :backend:test --tests "com.tamixa.performance.*" \
  --info \
  2>&1 | tee performance-test-results.log

echo ""
echo "========================================="
echo "Performance Test Summary"
echo "========================================="
echo ""

# Extract test results
if grep -q "BUILD SUCCESSFUL" performance-test-results.log; then
    echo "✅ All performance tests passed!"
    
    # Count tests
    TOTAL_TESTS=$(grep -c "Test.*PASSED" performance-test-results.log || echo "0")
    echo "Total tests executed: $TOTAL_TESTS"
    
    # Extract benchmark results
    echo ""
    echo "Benchmark Results:"
    grep -A 7 "Benchmark:" performance-test-results.log || echo "No benchmark results found"
    
else
    echo "❌ Some performance tests failed"
    
    # Show failures
    echo ""
    echo "Failed tests:"
    grep "Test.*FAILED" performance-test-results.log || echo "No failure details found"
fi

echo ""
echo "Full results saved to: performance-test-results.log"
echo ""
