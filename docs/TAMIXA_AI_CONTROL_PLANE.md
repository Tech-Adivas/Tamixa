# Tamixa AI Control Plane (index)

The **AI Control Plane** is the governance and orchestration layer **above** product features and agents: prompts, workflows, models, policies, evaluation, audit, and cost.

## Authoritative design

Full production architecture (modules, APIs, runtime flow, RBAC, phases): **[CONTROL_PLANE_ARCHITECTURE.md](CONTROL_PLANE_ARCHITECTURE.md)**  
**What is actually wired today (CI, DB, logs, security):** **[AI_GOVERNANCE_E2E.md](AI_GOVERNANCE_E2E.md)**

## What exists in this repo today

| Layer | Location |
|-------|----------|
| **PostgreSQL schema** | [backend/.../V75__ai_control_plane.sql](../backend/src/main/resources/db/migration/V75__ai_control_plane.sql) — `ai_*` tables + seed `ai_projects` row `code=tamixa` |
| **Kotlin ports (contracts)** | `com.tamixa.controlplane.application.port` — [ControlPlanePorts.kt](../backend/src/main/kotlin/com/tamixa/controlplane/application/port/ControlPlanePorts.kt) |
| **Process / docs bridge** | [CONTEXT_LIFECYCLE.md](CONTEXT_LIFECYCLE.md), [COST_GOVERNANCE.md](COST_GOVERNANCE.md), [MCP_POLICY.md](MCP_POLICY.md), [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md), [METRICS_EXPORT.md](METRICS_EXPORT.md) |

## Package naming

Runtime code uses **`com.tamixa.controlplane`** to match this monorepo. External design docs may say `com.techadivas.tamixa.controlplane` — treat that as an alias for the same logical module.

## Runtime spine (story generation)

Parent story generation (`StoryService.generate`) opens a workflow run when `app.control-plane.story-workflow-integration-enabled` is true (default in `application.yml`; tests override to false). Runs use `requestSource=STORY_SERVICE`, `entityType=STORY`, and complete or fail in `ai_workflow_runs` alongside the same admin-driven executes (`ADMIN_API`). See [AI_GOVERNANCE_E2E.md](AI_GOVERNANCE_E2E.md).

## Next implementation steps

See **§14 Phased delivery** and **§15 Build order** in [CONTROL_PLANE_ARCHITECTURE.md](CONTROL_PLANE_ARCHITECTURE.md): extend step runs, prompt composer, routing, policy, and cost on the hot path.

## Principle

Do **not** embed AI routing, prompt versioning, or policy logic only inside story/TTS services. Move invocations **behind** the control plane so every AI action can be versioned, routed, checked, evaluated, audited, and cost-tracked.
