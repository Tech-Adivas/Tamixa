# Tamixa repository instructions (GitHub Copilot)

This monorepo is the **Tamixa** multilingual storytelling platform: **Spring Boot** backend (`backend/`), **Kotlin Multiplatform** mobile (`mobile/`), **Next.js** admin (`admin/`), and **React/Vite** web (`web/`). AI-assisted development is **governed**: see [AGENTS.md](../AGENTS.md) for the required workflow, human gates, and links to SDLC prompts and policies.

## Canonical guidance (avoid drift)

- **Repo-wide agent playbook:** [AGENTS.md](../AGENTS.md)
- **Path-specific rules (Cursor):** [.cursor/rules/](../.cursor/rules/) — when instructions conflict, **the most specific path rule wins** (e.g. `backend/**` over this file).
- **Naming:** [.cursor/rules/naming-conventions.mdc](../.cursor/rules/naming-conventions.mdc) and [docs/NAMING_CONVENTIONS.md](../docs/NAMING_CONVENTIONS.md)
- **Security / PII / secrets:** [.cursor/rules/security-vulnerabilities.mdc](../.cursor/rules/security-vulnerabilities.mdc)
- **MCP and tools:** [docs/MCP_POLICY.md](../docs/MCP_POLICY.md)
- **Repeatable SDLC prompts:** [.github/prompts/](./prompts/) (includes `evaluate-output` for per-output scoring)
- **Evaluation, cost, lifecycle, boundaries, control plane:** [docs/AI_EVALUATION_SYSTEM.md](../docs/AI_EVALUATION_SYSTEM.md), [docs/COST_GOVERNANCE.md](../docs/COST_GOVERNANCE.md), [docs/CONTEXT_LIFECYCLE.md](../docs/CONTEXT_LIFECYCLE.md), [docs/AGENT_EXECUTION_BOUNDARIES.md](../docs/AGENT_EXECUTION_BOUNDARIES.md), [docs/TAMIXA_AI_CONTROL_PLANE.md](../docs/TAMIXA_AI_CONTROL_PLANE.md), [docs/CONTROL_PLANE_ARCHITECTURE.md](../docs/CONTROL_PLANE_ARCHITECTURE.md), [docs/AI_GOVERNANCE_E2E.md](../docs/AI_GOVERNANCE_E2E.md)

## Defaults for all changes

- Prefer **small, reviewable** changes with **tests** and **rollback-safe** Flyway migrations when the schema changes.
- Before writing non-trivial code, summarize: **objective**, **impacted modules**, **risks**, **tests**, **rollout/rollback**.
- **Never** change auth, IAM, secrets, payment logic, or deployment config without **explicitly** calling it out in the plan and getting **human approval** (see human gates in [AGENTS.md](../AGENTS.md)).
- Keep **product/runtime prompts** (story generation, moderation) in code and dedicated prompt docs; keep **SDLC prompts** under [.github/prompts/](./prompts/).
- Use **structured outputs** and validation for AI-facing components; default to **concise** responses and **minimal diffs** with clear test evidence.
- **Mobile-first** for product UX when touching user-facing flows ([.cursor/rules/mobile-first.mdc](../.cursor/rules/mobile-first.mdc)).

## Path-scoped instructions

GitHub path-scoped custom instructions (if enabled in your org) live under [.github/instructions/](./instructions/).
