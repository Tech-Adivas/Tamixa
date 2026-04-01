# Tamixa MCP policy (engineering)

**Version:** 1  
**Purpose:** Govern discovery and use of **Model Context Protocol (MCP)** servers and agent-connected tools across planning, design, development, testing, deployment, and support.

## Usage modes

| Mode | Description |
|------|-------------|
| **Read-only** | Documentation, issues, PRs, logs, dashboards — no writes |
| **Propose** | Create branches, draft PRs, suggest config changes — still subject to human review |
| **Act** | Irreversible or privileged operations — **disabled by default**

## Default stance

- MCP servers are **disabled by default** unless listed in the **approved registry** (maintain in team docs or internal wiki; reference this file from onboarding).
- Every **new MCP server** requires **threat review** and **owner approval** (Security + Tech lead per [AGENTIC_SDLC_GOVERNANCE.md](AGENTIC_SDLC_GOVERNANCE.md)).
- Tools must use **least-privilege** scopes and **short-lived** credentials where applicable.

## Approved categories (typical)

- **GitHub** MCP (read / propose only by default)
- **Internal docs / ADR** server (read-only)
- **CI artifacts / logs** (read-only)
- **Observability search** (read-only; **redact secrets**)

## Restricted categories (require explicit approval + logging)

- Production **database write** tools  
- **Secrets manager write** tools  
- **Direct deployment** or infrastructure **actuation**  
- **Billing / admin** tools with broad account access  

## Telemetry

Log **server**, **tool**, **user/agent identity** (as allowed by privacy policy), **scope**, **purpose**, and **outcome**. Alert on:

- Unapproved servers or tools  
- Scope expansion  
- Repeated failures or anomalous volume  

## Human approvals

Required for:

- Enabling a **new** server  
- Expanding **scopes** or moving from Read-only → Propose → Act  
- Any **Act**-mode usage  

## SDLC phase defaults

Align tool modes with the phase of work (planning → support). Summary:

| Phase | Default MCP mode | Notes |
|-------|------------------|--------|
| Planning | Read-only | Prefer summaries over raw dumps |
| Design | Read-only / Propose | Human approval before new external dependencies |
| Development | Read-only / Propose | Sandboxed execution; branch-scoped writes |
| Testing | Read-only / Propose | Redact secrets; cap payload size |
| Deployment | Propose at most | **Human approval** mandatory for production |
| Support | Read-only / Propose | No direct production actuation by default |

## Relation to product AI

**MCP governance** controls what engineering agents can **reach** (files, APIs, cloud). **Prompt governance** controls **language** sent to models in product features (e.g. story generation). Both apply; they are not interchangeable.

**Execution boundaries** (what agents must not do without approval) are defined in [AGENT_EXECUTION_BOUNDARIES.md](AGENT_EXECUTION_BOUNDARIES.md).

## References

- [AGENTS.md](../AGENTS.md) — workflow and human gates  
- [AGENTIC_SDLC_GOVERNANCE.md](AGENTIC_SDLC_GOVERNANCE.md) — RACI and maturity scorecard  
- OWASP LLM and MCP security guidance (external) — map controls during threat review  
