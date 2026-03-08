#!/usr/bin/env bash
# Load test simulation for 100 concurrent story narration jobs.
# Prerequisites: Backend running, valid JWT for NARRATION_LOAD_TEST_TOKEN.
# Usage: ./narration-load-test.sh [BASE_URL] [CONCURRENT] [REQUESTS_PER_BATCH]

set -e
BASE_URL="${1:-http://localhost:8080}"
CONCURRENT="${2:-100}"
BATCH="${3:-1}"

# Requires auth token - set NARRATION_LOAD_TEST_TOKEN or pass as 4th arg
TOKEN="${NARRATION_LOAD_TEST_TOKEN:-$4}"
if [ -z "$TOKEN" ]; then
  echo "Set NARRATION_LOAD_TEST_TOKEN or pass as 4th arg. Use a valid JWT for an authenticated parent."
  exit 1
fi

# Translation ID - must exist in DB; use a seed translation from test data
TRANSLATION_ID="${TRANSLATION_ID:-1}"

echo "Narration load test: $CONCURRENT concurrent, $BATCH requests each"
echo "Base URL: $BASE_URL"
echo "Translation ID: $TRANSLATION_ID"

start=$(date +%s.%N)
success=0
fail=0

# Fire concurrent requests (each runs in background)
for i in $(seq 1 $CONCURRENT); do
  for j in $(seq 1 $BATCH); do
    (
      resp=$(curl -s -w "%{http_code}" -o /tmp/narration_${i}_${j}.json \
        -X POST "${BASE_URL}/api/v1/narration/request" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"translationId\": $TRANSLATION_ID, \"toneMode\": \"CALM\", \"voiceProfiles\": [\"default\"]}" 2>/dev/null || echo "000")
      if [ "$resp" = "202" ] || [ "$resp" = "200" ]; then
        echo "OK" >> /tmp/narration_success_$$
      else
        echo "FAIL:$resp" >> /tmp/narration_fail_$$
      fi
    ) &
  done
done

wait
end=$(date +%s.%N)
elapsed=$(echo "$end - $start" | bc)

# Count results
[ -f /tmp/narration_success_$$ ] && success=$(wc -l < /tmp/narration_success_$$)
[ -f /tmp/narration_fail_$$ ] && fail=$(wc -l < /tmp/narration_fail_$$)
total=$((CONCURRENT * BATCH))

echo ""
echo "Results: $success success, $fail failed (total $total)"
echo "Elapsed: ${elapsed}s"
echo "Requests/sec: $(echo "scale=2; $total / $elapsed" | bc 2>/dev/null || echo "N/A")"

# Cleanup
rm -f /tmp/narration_success_$$ /tmp/narration_fail_$$ /tmp/narration_*.json

exit $([ $fail -eq 0 ] && echo 0 || echo 1)
