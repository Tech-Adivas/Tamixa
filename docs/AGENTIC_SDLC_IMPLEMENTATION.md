# Agentic SDLC Implementation in Tamixa

This document explains how Agentic SDLC is implemented in this application today: process, controls, runtime integration, and delivery workflow.

---

## 1) What "Agentic SDLC" means here

In Tamixa, Agentic SDLC means AI agents are used across the software lifecycle, but within explicit guardrails:

- plan and implement changes with structured prompts/rules
- enforce governance in CI/CD
- evaluate outputs continuously
- keep high-risk actions gated by humans

Core operating pattern:

`Think -> Research -> Plan -> Act -> Verify -> Report`

Reference: `AGENTS.md`.

---

## 2) Implementation layers

### A. Workspace execution rules (authoring-time controls)

Where: `AGENTS.md`, `.cursor/rules/`, `.github/copilot-instructions.md`, `.github/prompts/`

How it is implemented:

- repository-level instructions define allowed behavior, quality bar, and security constraints
- path- and task-specific rules constrain coding agents for backend, delivery, security, testing, review, etc.
- prompt templates standardize common SDLC tasks (plan feature, implement feature, generate tests, review security, prepare release)

Outcome:

- AI output quality is more consistent and auditable
- fewer ad-hoc one-off prompts and less process drift

---

### B. CI governance (merge-time controls)

Where: `.github/workflows/`

Implemented workflows:

- `spec-first-guard.yml`: enforces spec-first discipline on PRs (or explicit bypass controls)
- `pr-governance-metrics.yml`: parses governance fields in PRs, produces structured artifact
- `pr-metrics-export.yml`: exports merged PR governance metrics; optional webhook sink
- `ci.yml`: backend/web/admin build and test gates

Outcome:

- governance is not just documentation; it is enforced in pull-request and merge paths
- measurable SDLC metadata is captured for trend analysis

---

### C. Runtime AI governance spine (application-time controls)

Where:

- DB migrations: `backend/src/main/resources/db/migration/V75__ai_control_plane.sql`, `V76__ai_control_plane_seed_tamixa.sql`
- backend control plane APIs/entities/services under `backend/src/main/kotlin/com/tamixa/...controlplane...`
- architecture docs: `docs/AI_GOVERNANCE_E2E.md`, `docs/TAMIXA_AI_CONTROL_PLANE.md`, `docs/CONTROL_PLANE_ARCHITECTURE.md`

How it is implemented:

- control-plane schema stores workflows, runs, steps, evaluation, cost, and audit entities (`ai_*` tables)
- story generation integrates with control-plane workflow execution path
- run lifecycle is persisted (execute, complete, fail), with request source tagging (for example admin API vs story service)
- control-plane admin APIs are protected by RBAC permissions

Outcome:

- AI behavior is trackable and governable at runtime, not only during code review
- supports policy, auditability, and progressive hardening

---

### D. Observability and delivery reliability (operations-time controls)

Where:

- DORA backend + admin integration
- workflow automations in `.github/workflows/dora-*.yml`
- helper script `scripts/report-dora-event.sh`

How it is implemented:

- deployment and incident events are captured and aggregated
- delivery reliability metrics are visible in admin monitoring
- automation paths support manual, reusable workflow, repository dispatch, and workflow-run triggers

Outcome:

- continuous feedback loop from engineering process into operational performance

---

## 3) Human approval gates (explicitly retained)

Agentic SDLC in Tamixa does not remove human accountability.

Human approvals are required for:

- architecture and ADR decisions
- schema/migration decisions
- auth/IAM/secrets changes
- model/provider changes
- production deployment and rollback decisions

Reference: `AGENTS.md`, `docs/AGENT_EXECUTION_BOUNDARIES.md`.

---

## 4) End-to-end flow in practice

1. Engineer/agent starts from governed prompt/rule context.
2. Changes are produced with code + tests + docs updates.
3. PR CI enforces spec/governance + build/test quality.
4. Merge produces governance metrics artifacts/exports.
5. Runtime AI flows execute against control-plane backbone and persist run/audit data.
6. Delivery outcomes (DORA) feed back into monitoring and process improvement.

This creates a closed loop:

`Authoring controls -> CI controls -> Runtime controls -> Metrics feedback -> Better instructions and workflows`

---

## 5) Current maturity snapshot

Strong today:

- documented governance model and RACI
- CI-based governance enforcement
- control-plane schema + initial runtime integration
- DORA reliability instrumentation and automation hooks

Still evolving:

- deeper step-level orchestration on all AI hot paths
- broader evaluator integration in runtime flow
- richer trend dashboards and policy automation

---

## 6) Related docs

- `AGENTS.md`
- `docs/AGENTIC_SDLC_GOVERNANCE.md`
- `docs/AI_GOVERNANCE_E2E.md`
- `docs/TAMIXA_AI_CONTROL_PLANE.md`
- `docs/CONTROL_PLANE_ARCHITECTURE.md`
- `docs/AI_EVALUATION_SYSTEM.md`
- `docs/COST_GOVERNANCE.md`
- `docs/MCP_POLICY.md`
- `docs/METRICS_EXPORT.md`
- `docs/DORA_METRICS_FRAMEWORK.md`
