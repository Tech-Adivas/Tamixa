#!/usr/bin/env bash
# Local Phase 1 checks from docs/ENTERPRISE_ROADMAP.md (complement to CI).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Checking .env is not tracked by git"
if [[ -n "$(git ls-files -- .env 2>/dev/null || true)" ]]; then
  echo "ERROR: .env is tracked. Remove it from git and keep it gitignored."
  exit 1
fi
echo "    OK"

echo "==> Backend compile + tests (backend-only)"
chmod +x ./gradlew 2>/dev/null || true
./gradlew :backend:compileKotlin :backend:test -Ptamixa.backendOnly=true --no-daemon

echo "==> Optional AI metrics smoke check"
if [[ -n "${API_BASE_URL:-}" && -n "${ADMIN_JWT:-}" ]]; then
  if ! command -v curl >/dev/null 2>&1; then
    echo "ERROR: curl is required for AI metrics smoke check."
    exit 1
  fi
  if ! command -v jq >/dev/null 2>&1; then
    echo "ERROR: jq is required for AI metrics smoke check."
    exit 1
  fi

  curl -sS \
    -H "Authorization: Bearer ${ADMIN_JWT}" \
    -H "Accept: application/json" \
    "${API_BASE_URL}/api/v1/admin/metrics/ai" \
    | jq -e '
        has("storyGenerationsTotal") and
        has("cacheHits") and
        has("cacheMisses") and
        has("voiceProcessingCount") and
        has("openaiTokensUsed")
      ' >/dev/null
  echo "    OK (AI metrics endpoint shape validated)"
else
  echo "    Skipped (set API_BASE_URL and ADMIN_JWT to enable)"
fi

echo ""
echo "Phase 1 local verification passed."
