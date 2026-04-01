# Feature specs (mini-specs)

Use this folder for **mini-specs** before implementation when **spec mode** applies (see Agentic SDLC blueprint, Section 6.1).

## Spec-first enforcement (elite teams)

For the areas below, the rule is: **no substantial implementation without an approved mini-spec** linked from the PR. Agents should **stop and ask for a spec** rather than inventing auth, payment, or pipeline behavior.

**Applies to:**

- **Database schema** or migration strategy  
- **Authentication**, **authorization**, **IAM**, or **session** behavior  
- **Payments**, **subscriptions**, **webhooks**, or **billing**  
- **Voice cloning**, **consent**, or **minor-related** data handling  
- **AI story pipeline** (prompt assembly, guardrails, provider routing, moderation hooks)  
- **External provider** integrations (AI, TTS, video, email, push)  
- **Deployment**, **feature flags**, or **environment** behavior that affects production  

“Substantial” means more than a typo fix or a clearly scoped one-line config comment. When in doubt, write a short spec.

For early exploration (“vibe mode”), a spec is optional; once the above surfaces appear, **switch to spec mode** immediately.

## Mini-spec contents

Each spec should cover:

1. **Objective**  
2. **In scope / out of scope**  
3. **Impacted modules** (paths)  
4. **Architecture / API / data impact**  
5. **Risks and assumptions**  
6. **Test plan**  
7. **Rollout and rollback**  
8. **Open questions** for reviewers  

You can start from the prompt [.github/prompts/plan-feature.prompt.md](../../.github/prompts/plan-feature.prompt.md).

## Naming

`feature-<slug>.md` or `YYYY-MM-<slug>.md` — pick one convention per team and stay consistent.

## CI enforcement

On pull requests to `main` / `develop`, **[`.github/workflows/spec-first-guard.yml`](../../.github/workflows/spec-first-guard.yml)** runs **[`.github/scripts/spec-first-guard.sh`](../../.github/scripts/spec-first-guard.sh)**. If mandatory paths change, the PR description must contain **`docs/specs/`** (your mini-spec link).

**Maintainer bypass:** apply GitHub label **`skip-spec-first`** (use sparingly).

**Emergency:** a line **`SPEC_BREAKGLASS: <reason>`** in the PR body skips the failure (emits a workflow warning; require follow-up spec/ADR).

**Low-touch workflow tweaks** (metrics/labels only) do **not** require a spec: `bootstrap-repo-labels.yml`, `pr-governance-metrics.yml`, `pr-metrics-export.yml`, and edits to `spec-first-guard.yml` itself are excluded in [spec-first-guard.sh](../../.github/scripts/spec-first-guard.sh). Everything else under `.github/workflows/` still triggers the guard.

## Related

- [docs/adr/README.md](../adr/README.md) — record **decisions** after the spec converges  
- [AGENTS.md](../../AGENTS.md) — governed workflow, VERIFY, boundaries  
- [docs/AGENT_EXECUTION_BOUNDARIES.md](../AGENT_EXECUTION_BOUNDARIES.md) — spec-first guardrail for agents  
- [`.github/workflows/pr-governance-metrics.yml`](../../.github/workflows/pr-governance-metrics.yml) — optional PR body metrics artifact  
- [docs/METRICS_EXPORT.md](../METRICS_EXPORT.md) — **`skip-spec-first` bootstrap**, webhook → Sheets/BigQuery  

