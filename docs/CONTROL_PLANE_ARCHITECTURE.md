# Tamixa AI Control Plane — production architecture

This document is the **governed design** for the system that sits **above** product features and coding agents: prompts, models, workflows, policies, evaluations, audits, and cost—not the story consumer app itself.

**Implementation status:** PostgreSQL schema in [V75__ai_control_plane.sql](../backend/src/main/resources/db/migration/V75__ai_control_plane.sql); Kotlin **port contracts** in `com.tamixa.controlplane.application.port` (no HTTP surface yet; **`/api/control-plane/**` is `denyAll` in `SecurityConfig` until RBAC ships**). **End-to-end map (CI + DB + runtime):** [AI_GOVERNANCE_E2E.md](AI_GOVERNANCE_E2E.md). Operational bridges: [TAMIXA_AI_CONTROL_PLANE.md](TAMIXA_AI_CONTROL_PLANE.md), [METRICS_EXPORT.md](METRICS_EXPORT.md), [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md).

---

## 1. Purpose

The control plane **decides**:

- Which **workflow** runs  
- Which **prompt / version** to use  
- Which **model / provider** to call  
- What **context** is injected  
- Which **policy** checks apply  
- How **output** is validated and **quality** scored  
- When **human review** is required  
- How **cost** and **performance** are tracked  

Without this layer, AI behavior fragments across services and repos.

---

## 2. Design principles

| Principle | Meaning |
|-----------|---------|
| Centralized governance | One source of truth for prompts, routes, policies, audits |
| Decentralized execution | Workers and product services **invoke** the plane; they don’t each embed routing logic |
| API-first | All governance operations go through versioned APIs |
| Versioned | Prompts, workflows, policies are immutable versions + approvals |
| Auditable | Every change and run is traceable |
| Multilingual-aware | Language profiles and packs are first-class |
| Human-gated | Sensitive paths require explicit approval |
| Provider-agnostic | Adapters behind stable ports |

---

## 3. High-level architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    TAMIXA AI CONTROL PLANE                    │
├──────────────────────────────────────────────────────────────┤
│  Control API     Prompt / Workflow / Policy / Evaluation /   │
│                  Cost & Metrics APIs                          │
├──────────────────────────────────────────────────────────────┤
│  Orchestration   Workflow engine, Prompt composer, Model     │
│                  router, Policy engine, Validation, Human    │
│                  review router                                │
├──────────────────────────────────────────────────────────────┤
│  Intelligence    Context manager, Language profiles,         │
│                  Evaluation, Experimentation, Learning         │
├──────────────────────────────────────────────────────────────┤
│  Integration     LLM / Translation / TTS / Voice / Avatar /   │
│                  MCP adapters                                  │
├──────────────────────────────────────────────────────────────┤
│  Data            PostgreSQL (SoT), Redis (cache/locks),       │
│                  object storage, analytics store               │
├──────────────────────────────────────────────────────────────┤
│  Governance      Audit, approval, RBAC, policy enforcement   │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. Bounded modules

| Module | Responsibility |
|--------|----------------|
| **Prompt registry** | System prompts, seeds, templates, schemas, language/cultural packs — versioned |
| **Workflow registry** | Story generation, polish, translation, moderation, narration prep, avatar script, SDLC flows |
| **Model router** | Provider, model, fallback, cost tier |
| **Context manager** | Final context package from user input, metadata, language profile, policy flags, memory |
| **Policy engine** | Child safety, injection, voice consent, provider/MCP restrictions |
| **Evaluation engine** | Structure, fluency, multilingual quality, emotion, narration readiness, safety |
| **Audit + review** | Every run; escalate uncertain outputs |

---

## 5. Runtime flow (example)

**Goal:** Tamil bedtime story with narration.

1. Product calls `POST /api/control-plane/v1/workflows/execute` with `workflowKey`, `input` (language, audience, theme, …).  
2. Workflow engine resolves `story.create_with_narration`.  
3. Context manager loads preferences, audience profile, **language profile**, seeds.  
4. Prompt composer assembles system prompt, language seed, task template, **output schema**.  
5. Model router selects provider/model + fallbacks.  
6. Policy engine runs safety and allow-lists.  
7. Provider adapter executes.  
8. Validation engine checks schema/output.  
9. Evaluation engine scores; if below threshold → rewrite or **human review**.  
10. Persist run, cost, prompt versions, output, scores.  
11. Return result + run/step ids.

---

## 6. Database (PostgreSQL)

**Source of truth:** tables prefixed with `ai_` (see Flyway **V75**).

| Table | Role |
|-------|------|
| `ai_projects` | Tenant / product boundary |
| `ai_workflows`, `ai_workflow_steps` | Workflow definitions (JSONB) |
| `ai_prompt_assets`, `ai_prompt_asset_versions` | Prompt registry + approvals |
| `ai_language_profiles` | Per-locale capabilities and thresholds |
| `ai_model_providers`, `ai_models` | Provider catalog |
| `ai_routing_policies` | Task/language/tier → model rules |
| `ai_policy_rules` | Block/allow/review rules |
| `ai_workflow_runs`, `ai_workflow_step_runs` | Execution audit trail |
| `ai_evaluation_profiles`, `ai_evaluation_results` | Rubrics and scores |
| `ai_human_review_queue` | Escalations |
| `ai_cost_events` | Token/cost/latency per step |
| `ai_audit_events` | Governance actions |

**Redis (later):** workflow locks, short-lived context cache, health cache, composition cache, rate limits — **not** system of record.

---

## 7. API surface (v1 target)

Base path: `/api/control-plane/v1`

| Area | Endpoints (representative) |
|------|----------------------------|
| Workflows | `POST .../workflows/execute`, `GET .../workflow-runs/{id}`, `POST .../workflow-runs/{id}/retry`, `POST .../cancel` |
| Prompts | `POST .../prompt-assets`, `POST .../prompt-assets/{id}/versions`, `POST .../versions/{v}/approve`, `GET .../prompt-assets/{assetKey}`, `GET .../compare` |
| Languages | `GET .../languages`, `GET/PUT .../languages/{code}` |
| Routing | `POST .../routing/resolve` |
| Policies | `POST .../policies/evaluate`, `GET/POST .../policies/rules` |
| Evaluation | `POST .../evaluations/run`, `GET .../evaluations/{id}`, `GET .../evaluation-profiles` |
| Reviews | `POST .../reviews/queue`, `POST .../reviews/{id}/resolve` |
| Usage | `GET .../usage/summary`, `GET .../costs/by-task`, `GET .../models/performance` |

Request/response shapes follow the JSON examples in the design spec (projectCode, workflowKey, routing resolve, etc.).

---

## 8. Workflow definition (JSONB)

Workflows are stored in `ai_workflows.definition_json` (and optionally normalized in `ai_workflow_steps`). Example step types: `CONTEXT`, `MODEL_CALL`, `POLICY`, `ASYNC_JOB`, with keys like `promptAssetKey`, `evaluationProfileKey`, `fallbackStepKey`.

---

## 9. Policy rule types (examples)

`MODEL_ALLOWLIST`, `LANGUAGE_RESTRICTION`, `TOOL_ALLOWLIST`, `VOICE_CONSENT_REQUIRED`, `CHILD_SAFETY_THRESHOLD`, `MAX_COST_PER_RUN`, `HUMAN_REVIEW_REQUIRED`, `NO_PRODUCTION_DEPLOY_FROM_AGENT`, …

Rules use `condition_json` + `action_json` in `ai_policy_rules`.

---

## 10. Evaluation dimensions (by task)

- **Story:** structure, fluency, emotional clarity, age fit, cultural fit, multilingual naturalness  
- **Translation:** meaning, fluency, idiom, tone, script  
- **TTS script:** speakability, pauses, pronunciation, pacing  
- **SDLC code review:** correctness, maintainability, tests, security, architecture  

Stored as `rubric_json` / `dimension_scores_json`.

---

## 11. RBAC (target)

Roles: `AI_ADMIN`, `PROMPT_MANAGER`, `WORKFLOW_MANAGER`, `POLICY_MANAGER`, `REVIEWER`, `READONLY_ANALYST`, `PLATFORM_OPERATOR` — map to Spring Security + admin parent roles or dedicated control-plane principals.

---

## 12. Security

- Service-to-service auth (signed tokens / mTLS)  
- RBAC on admin APIs  
- Secrets in vault; never raw secrets in `input_json`  
- Prompt publish/approve workflow  
- Deny-by-default model/tool policy  
- Redacted logs; consent references for voice clone  
- Agent deploy rules aligned with [AGENT_EXECUTION_BOUNDARIES.md](AGENT_EXECUTION_BOUNDARIES.md)  

---

## 13. Deployment shape (recommended)

| Unit | Role |
|------|------|
| **Control API** | Gateway: auth, RBAC, tracing, rate limits |
| **Workflow worker** | Long-running / async steps |
| **Evaluation worker** | Batch or async scoring |
| **Admin UI** | Prompt/workflow/policy/review consoles |

Near term: **logical module** inside existing backend (`com.tamixa.controlplane`), split to separate deployable later.

---

## 14. Phased delivery

| Phase | Scope |
|-------|--------|
| **1** | Schema ✅, prompt registry APIs, workflow execute + runs, model router, policy engine, cost tracking |
| **2** | Evaluation engine, human review queue, language profiles, A/B |
| **3** | Learning/optimization, prompt ranking, provider loop |

**Build order:** schema → prompt registry → workflow execution API → routing → policy → evaluation → review UI → admin dashboard.

---

## 15. Related

- Kotlin ports: `com.tamixa.controlplane.application.port`  
- [COST_GOVERNANCE.md](COST_GOVERNANCE.md) · [MCP_POLICY.md](MCP_POLICY.md) · [CONTEXT_LIFECYCLE.md](CONTEXT_LIFECYCLE.md)  
