# Mini-spec: Agentic CI guardrails and governance metrics

**Status:** Accepted  
**Date:** 2026-03-24  

## Objective

Enforce **spec-first** links on high-blast-radius backend and workflow changes, capture optional **PR governance metrics** for AI SDLC rollups, and emit structured **ai_governance** logs when a generated story is persisted.

## In scope

- `.github/workflows/spec-first-guard.yml` + `.github/scripts/spec-first-guard.sh`
- `.github/workflows/pr-governance-metrics.yml` + `.github/scripts/parse_pr_governance.py`
- `.github/workflows/bootstrap-repo-labels.yml` (creates `skip-spec-first` via Actions UI)
- `.github/workflows/pr-metrics-export.yml` + `.github/scripts/enrich_pr_metrics_export.py` + optional webhook secrets
- Optional label `skip-spec-first` for maintainers
- `SPEC_BREAKGLASS:` line in PR body for emergencies (with mandatory follow-up)
- Story completion log fields in `StoryService` for observability / evaluation pipelines

## Out of scope

- Central “control plane” service, cost billing APIs, or automated model routing
- Blocking PRs on missing `eval-pass` (informational metrics only)

## Impacted modules

- `.github/workflows/`, `.github/scripts/`
- `backend/.../story/StoryService.kt`

## Risks

- **False positives:** new sensitive paths may need adding to the guard script.
- **Fork PRs:** `pull_request` workflow runs with same-repo permissions; fork PRs get read-only checks (standard).

## Test plan

- Open a PR that only changes `README.md` → guard passes.
- Open a PR that touches `SecurityConfig.kt` without `docs/specs/` in body → guard fails.
- Same PR with `docs/specs/feature-agentic-ci-guardrails.md` linked in body → guard passes.

## Rollout / rollback

- Merge workflows; disable by removing workflow file or adding org-wide exception.
- Rollback StoryService change: remove `logAiGovernanceStoryCompleted` call and method.

## Rollback

Revert PR; remove workflows if causing noise.
