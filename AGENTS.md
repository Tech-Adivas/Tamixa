# Tamixa Project Guidelines for AI & Developers

This document guides AI agents and developers toward consistent, professional, and secure code.

## Governed workflow (Think → Research → Plan → Act → Verify → Report)

For **non-trivial** work, follow this split to avoid code-first drift. Trivial fixes (typos, one-line obvious bugs) may skip straight to **Act** with a short **Report**.

| Stage | Primary outcome | Typical AI activities | Human checkpoint |
|-------|-----------------|------------------------|------------------|
| **Think** | Problem framing | Clarify objective, constraints, task type | Approve framing |
| **Research** | Evidence base | Read minimal code, docs, tickets, ADRs, specs | Approve evidence sufficiency |
| **Plan** | Safe implementation path | Propose design, tests, rollout, rollback, risks | Approve plan |
| **Act** | Executable change | Implement, test, document; run checks in sandbox | Review PR |
| **Verify** | Quality gate on AI output | Per-output eval (story/code/tests/PR), security pass, CI | See [docs/AI_EVALUATION_SYSTEM.md](docs/AI_EVALUATION_SYSTEM.md) |
| **Report** | Traceability | Summarize changes, evidence, open risks | As part of review |

**Task classification (route model + agent):** THINK → RESEARCH → PLAN → ACT → VERIFY — see [docs/AGENT_EXECUTION_BOUNDARIES.md](docs/AGENT_EXECUTION_BOUNDARIES.md) and [docs/COST_GOVERNANCE.md](docs/COST_GOVERNANCE.md).

**Where agents STOP (cannot without explicit approval):** auth/IAM, secrets, production deploy, infra actuation, Act-mode MCP — full matrix in [docs/AGENT_EXECUTION_BOUNDARIES.md](docs/AGENT_EXECUTION_BOUNDARIES.md).

**Repeatable SDLC prompts** (copy into chat or Copilot): [.github/prompts/](.github/prompts/) — `plan-feature`, `implement-feature`, `generate-tests`, `review-security`, `prepare-release`, `evaluate-output`.

## Context lifecycle

Instructions and prompts **age**. Follow **Create → Validate → Use → Measure → Update → Deprecate** with versioning, owners, review dates, and metrics — [docs/CONTEXT_LIFECYCLE.md](docs/CONTEXT_LIFECYCLE.md).

**Persistent context (“Engineering Brain”):** [docs/engineering-brain/README.md](docs/engineering-brain/README.md), ADRs, specs, [docs/context-packs/](docs/context-packs/README.md).

## AI evaluation, cost, and control plane

- **Per-output evaluation + learning loop:** [docs/AI_EVALUATION_SYSTEM.md](docs/AI_EVALUATION_SYSTEM.md) — use **Evaluation** agent (`.cursor/rules/evaluation-agent.mdc`) on meaningful AI artifacts, not only quarterly scorecards.  
- **Cost governance (task → model → tier):** [docs/COST_GOVERNANCE.md](docs/COST_GOVERNANCE.md).  
- **Control plane (index + full architecture + E2E map):** [docs/TAMIXA_AI_CONTROL_PLANE.md](docs/TAMIXA_AI_CONTROL_PLANE.md), [docs/CONTROL_PLANE_ARCHITECTURE.md](docs/CONTROL_PLANE_ARCHITECTURE.md), [docs/AI_GOVERNANCE_E2E.md](docs/AI_GOVERNANCE_E2E.md).

## Human gates

Agentic SDLC does **not** remove accountability. Human approval is required before irreversible or high-blast-radius steps.

| Decision type | Human required? | Reason |
|---------------|-----------------|--------|
| Architecture / ADR approval | **Yes** | Long-lived blast radius |
| Schema / migrations | **Yes** | Data integrity, rollback complexity |
| Auth / IAM / secrets | **Yes** | Security-critical |
| Model or provider changes | **Yes** | Behavior, cost, compliance |
| Production deployment | **Yes** | Operational accountability |
| Docs / test-only changes | **Usually** (lightweight) | Still reviewed via PR |
| Issue triage / suggestions | Optional | Automation-friendly |

## Instruction precedence (avoid drift)

1. **Most specific path rule wins** — e.g. `.github/instructions/backend.instructions.md` for `backend/**`, or the relevant `.cursor/rules/*.mdc` agent rule.  
2. **Repo-wide** — [.github/copilot-instructions.md](.github/copilot-instructions.md) (GitHub Copilot) and this file.  
3. **Always-on Cursor rules** — security, professional standards, mobile-first, naming.  

If two sources conflict, prefer the **narrower scope** and escalate in PR discussion.

## SDLC artifacts vs product prompts

- **SDLC** (planning, tests, review, release): [.github/prompts/](.github/prompts/), [docs/specs/](docs/specs/README.md), [docs/adr/](docs/adr/README.md).  
- **Product / runtime** (story generation, narration, moderation): backend code (e.g. `StoryPromptBuilder`), [.cursor/rules/prompt-story-agent.mdc](.cursor/rules/prompt-story-agent.mdc).  

## Governance and MCP

- **MCP and agent tools:** [docs/MCP_POLICY.md](docs/MCP_POLICY.md) — allow-list, modes, approvals.  
- **RACI, maturity scorecard, rolling metrics:** [docs/AGENTIC_SDLC_GOVERNANCE.md](docs/AGENTIC_SDLC_GOVERNANCE.md) — quarterly scorecard **plus** per-output eval and PR acceptance tracking ([docs/AI_EVALUATION_SYSTEM.md](docs/AI_EVALUATION_SYSTEM.md)).  

**Deployment:** Agents prepare changes; **only CI/CD and authorized humans** deploy to production — never uncontrolled direct agent deploy.

## Project Overview

- **Mobile app** (KMP, Compose) – Primary product; kid + parent experience
- **Backend** (Kotlin, Spring Boot) – REST API, PostgreSQL, Redis, S3
- **Admin** (Next.js) – Dashboard for content ops
- **Web** (React, Vite) – Parent-facing web app
- **Optional platform deployment** – Some environments use a **separate** repo for gateway, notifications, and multi-service Compose. **This monorepo has no build-time or CI dependency on that stack**; see [docs/MICROSERVICES_PLATFORM.md](docs/MICROSERVICES_PLATFORM.md) for the boundary.

## Priorities

1. **Mobile-first** – Per `.cursor/rules/mobile-first.mdc`
2. **Professional quality** – Per `.cursor/rules/professional-standards.mdc`
3. **Security** – Per `.cursor/rules/security-vulnerabilities.mdc`
4. **Naming conventions** – Per `.cursor/rules/naming-conventions.mdc` (backend, mobile, web, admin). See also [docs/NAMING_CONVENTIONS.md](docs/NAMING_CONVENTIONS.md) for a standalone reference.

## Specialized Agents

Use these rules when performing the corresponding tasks (enable in rule picker or reference in prompts). **For a detailed guide with sample prompts, see [docs/CURSOR_AGENTS_GUIDE.md](docs/CURSOR_AGENTS_GUIDE.md).**

| Agent | Rule | Use when |
|-------|------|----------|
| **Review** | `.cursor/rules/review-agent.mdc` | Code review, PR feedback, merge checklist |
| **Evaluation** | `.cursor/rules/evaluation-agent.mdc` | Per-output scores (story, multilingual, code/tests, PR); learning-loop feedback |
| **Testing** | `.cursor/rules/testing-agent.mdc` | Adding tests, running tests, test patterns |
| **Delivery** | `.cursor/rules/delivery-agent.mdc` | CI/CD changes, release, deployment checklist |
| **Security** | `.cursor/rules/security-agent.mdc` | Security review, audit, vulnerability checks |
| **Prompt / Story** | `.cursor/rules/prompt-story-agent.mdc` | Story/narration prompts, Tamixa persona, safety, StoryPromptBuilder |
| **Refactoring** | `.cursor/rules/refactoring-agent.mdc` | Refactors, tech debt, in-place changes, incremental steps |
| **Documentation** | `.cursor/rules/documentation-agent.mdc` | README, API docs, runbooks, .env.example |
| **API Design** | `.cursor/rules/api-design-agent.mdc` | New/changed REST endpoints, DTOs, OpenAPI, status codes |
| **Database / Migrations** | `.cursor/rules/database-migrations-agent.mdc` | Flyway migrations, schema changes, backward compatibility |
| **Observability** | `.cursor/rules/observability-agent.mdc` | Logging, metrics (ApplicationMetrics), actuator, tracing |
| **Code Validation** | `.cursor/rules/code-validation-agent.mdc` | Validate a file, its references, and alignment with project rules |
| **Figma / Design** | `.cursor/rules/figma-design-agent.mdc` | Verify UI implementation matches Figma or design spec |

## Code Review Checklist

Before merging, verify:

- [ ] No secrets or PII in logs
- [ ] Input validation on new endpoints
- [ ] Error handling with logging (no silent catches)
- [ ] Authorization checks on protected resources
- [ ] Tests for critical paths
- [ ] No `@Profile("dev")` logic enabled in production

## Architecture Conventions

- **Backend**: Hexagonal-ish – controllers, services, ports (interfaces), adapters (implementations)
- **Mobile**: KMP shared → `commonMain`, `androidMain`, `iosMain`
- **Admin/Web**: Component-based; use existing UI tokens and themes

## When Making Changes

1. Refactor in place; avoid creating redundant files.
2. Preserve existing patterns (repository ports, DTO naming).
3. Add logging for new flows; never log secrets.
4. Run tests before committing: `./gradlew :backend:test -Ptamixa.backendOnly=true` (alias: `-Pararo.backendOnly=true`)
