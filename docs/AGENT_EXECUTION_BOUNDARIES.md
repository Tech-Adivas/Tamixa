# Agent execution boundaries (where agents STOP)

Think → Research → Plan → Act is **not** unbounded. This document defines **allowed** vs **forbidden** behavior so agents do not modify the wrong modules, break architecture, or bypass standards.

## Global rules

| Agents MAY | Agents MUST NOT (without explicit human approval documented in PR/spec) |
|------------|---------------------------------------------------------------------------|
| Read the repo, docs, ADRs, specs, logs (redacted) | **Change auth/IAM**, security filters, or session model |
| Propose edits via patches / PRs | **Deploy** to any environment or trigger production infra |
| Generate tests and run **local/sandbox** commands | **Change production** secrets, IAM, billing keys, or live feature flags |
| Use **Read-only / Propose** MCP per [MCP_POLICY.md](MCP_POLICY.md) | **Act-mode** MCP (destructive or prod write tools) |
| Follow layer boundaries (controller → service → port) | **Bypass** validation, logging standards, or `@PreAuthorize` patterns |
| Touch `infra/`, workflows, Terraform/K8s only if tasked and approved | **Introduce** new external providers or keys without ADR + security review |

**Secrets:** agents never read real `.env`; use `.env.example` only.

## Task classification → routing

Classify every task before heavy model use; route to the right **agent rule** and **model tier** ([COST_GOVERNANCE.md](COST_GOVERNANCE.md)).

| Class | Examples | Typical agent | VERIFY |
|-------|----------|---------------|--------|
| **THINK** | Architecture, trade-offs, risk framing | Human + optional AI; ADR if decision sticks | Human approves framing |
| **RESEARCH** | Repo analysis, “where is X” | Code Validation, Review (read-only) | Cite files |
| **PLAN** | Design, API sketch, migration plan | API Design, Database/Migrations | Spec/ADR link |
| **ACT** | Code, tests, docs | Implement after approved plan | CI green |
| **VERIFY** | Eval scores, security pass, eval rubric | **Evaluation**, Security, Review | Per-output eval + merge checklist |

## Agent capability matrix

| Agent | Allowed | Restricted / forbidden |
|-------|---------|-------------------------|
| **Implementation (default coding)** | Code, tests, docs in scoped paths | Auth, payments, prod config, deploy |
| **Security** | Scan, report, recommend | Apply “fixes” that weaken controls without review |
| **Delivery** | CI workflow edits, runbooks | Production deploy actuation |
| **Evaluation** | Score outputs, suggest prompt/rule updates | Merge PRs, change prod |
| **Prompt / Story** | Story prompts, language hints | Shipping without safety + Evaluation pass for high-risk copy |
| **Database / Migrations** | Migrations in repo | Destructive prod DDL via tools |

If a task spans agents, **sequence** them: PLAN (API Design) → ACT (coding agent) → VERIFY (Evaluation + Review).

## Spec-first guardrail

For **schema, auth, AI pipeline, voice cloning, payments**, there is **no code without an approved mini-spec** — see [specs/README.md](specs/README.md). Agents should **refuse** to implement large changes in these areas if the spec link is missing (escalate to human).

## Related

- [AGENTS.md](../AGENTS.md) — workflow and human gates  
- [MCP_POLICY.md](MCP_POLICY.md) — tool modes  
- [TAMIXA_AI_CONTROL_PLANE.md](TAMIXA_AI_CONTROL_PLANE.md) — policy + audit target state  
