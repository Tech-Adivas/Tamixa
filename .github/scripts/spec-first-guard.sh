#!/usr/bin/env bash
# Fails if the PR changes spec-mandatory paths without linking a mini-spec or explicit break-glass.
# See docs/specs/README.md
set -euo pipefail

: "${BASE_SHA:?Set BASE_SHA (e.g. github.event.pull_request.base.sha)}"
: "${HEAD_SHA:?Set HEAD_SHA (e.g. github.event.pull_request.head.sha)}"
if [[ -n "${PR_BODY_FILE:-}" ]]; then
  PR_BODY="$(cat "$PR_BODY_FILE")"
else
  PR_BODY="${PR_BODY:-}"
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "spec-first-guard: not a git repository" >&2
  exit 1
fi

matches_spec_required() {
  local f=$1
  case "$f" in
    backend/src/main/resources/db/migration/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/api/config/SecurityConfig.kt) return 0 ;;
    backend/src/main/kotlin/com/tamixa/api/config/AdminAuth.kt) return 0 ;;
    backend/src/main/kotlin/com/tamixa/infrastructure/stripe/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/api/webhook/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/infrastructure/webhook/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/infrastructure/config/WebhookEncryptionValidator.kt) return 0 ;;
    backend/src/main/kotlin/com/tamixa/application/voice/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/application/story/StoryPromptBuilder.kt) return 0 ;;
    backend/src/main/kotlin/com/tamixa/application/story/StoryPromptTemplates.kt) return 0 ;;
    backend/src/main/kotlin/com/tamixa/application/guardrail/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/application/port/StructuredStoryRemoteGuardrailPort.kt) return 0 ;;
    backend/src/main/kotlin/com/tamixa/infrastructure/guardrails/*) return 0 ;;
    backend/src/main/kotlin/com/tamixa/controlplane/*) return 0 ;;
    .github/workflows/*)
      case "$f" in
        .github/workflows/spec-first-guard.yml) return 1 ;;
        .github/workflows/bootstrap-repo-labels.yml) return 1 ;;
        .github/workflows/pr-governance-metrics.yml) return 1 ;;
        .github/workflows/pr-metrics-export.yml) return 1 ;;
        *) return 0 ;;
      esac
      ;;
  esac
  return 1
}

body_has_spec_reference() {
  local b=$1
  [[ "${b,,}" == *"docs/specs/"* ]]
}

body_has_breakglass() {
  local b=$1
  grep -qE '^[[:space:]]*SPEC_BREAKGLASS:[[:space:]]+.+' <<<"$b"
}

mapfile -t changed < <(git diff --name-only "$BASE_SHA" "$HEAD_SHA" || true)
required_hits=()
for f in "${changed[@]}"; do
  [[ -z "$f" ]] && continue
  if matches_spec_required "$f"; then
    required_hits+=("$f")
  fi
done

if ((${#required_hits[@]} == 0)); then
  echo "spec-first-guard: no spec-mandatory paths changed — OK"
  exit 0
fi

echo "spec-first-guard: spec-mandatory paths touched:"
printf '  - %s\n' "${required_hits[@]}"

if body_has_spec_reference "$PR_BODY"; then
  echo "spec-first-guard: PR body references docs/specs/ — OK"
  exit 0
fi

if body_has_breakglass "$PR_BODY"; then
  echo "::warning::spec-first-guard: SPEC_BREAKGLASS used — ensure post-incident review and ADR/spec update."
  exit 0
fi

echo "::error::spec-first-guard: Add a mini-spec under docs/specs/ and link it in the PR description (must contain 'docs/specs/')," \
  "or use SPEC_BREAKGLASS: <reason> only for emergencies (requires follow-up)." >&2
echo "See docs/specs/README.md" >&2
exit 1
