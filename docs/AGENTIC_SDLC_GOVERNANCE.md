# Agentic SDLC governance (Tamixa / araro-kids)

This document **operationalizes** the Tamixa Agentic SDLC blueprint: **RACI**, **maturity scorecard**, and pointers to **metrics**. Owners run reviews **quarterly**; update this file with the latest scores and notes.

**Faster feedback:** Per-output evaluation and PR acceptance tracking run **continuously** (each meaningful AI artifact / PR), not only at quarter end — see [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md).

## Maturity scorecard (quarterly)

Assess each dimension from **1** (weak) to **5** (strong). Average the scores; use the **lowest two** dimensions to set next quarter’s improvement agenda. Target near-term state: **Level 3 — governed agentic** (scoped build/test PR workflows within policy).

| Dimension | Score 1 | Score 3 | Score 5 |
|-----------|---------|---------|---------|
| **Instructions & prompts** | Ad hoc chat only | Repo/path instructions + SDLC prompt files ([.github/prompts/](../.github/prompts/), [.cursor/rules/](../.cursor/rules/)) | Versioned prompt registry with owners, evals, retirement policy |
| **Context management** | Too much or too little context | Scoped repo context + basic retrieval | Context packs, redaction, summaries, bounded MCP retrieval |
| **Agent autonomy** | Uncontrolled | Scoped build/test PR workflows | Policy-based autonomy, approval tiers, rollback evidence |
| **Security controls** | Generic scanning only | AI/MCP threat model + secret scanning + least privilege | Full telemetry, red-team program, control testing |
| **Quality evaluation** | Manual spot checks | Required tests + basic review rubric | Automated evaluator scores with trends |
| **Cost governance** | No visibility | Token/cost visibility by task class | Model routing, cache strategy, cost SLOs |
| **Deployment safety** | Inconsistent | Protected branches + CI | Canary, rollout policy, rollback rehearsals, audited approvals |
| **Learning loop** | No reuse of lessons | Postmortems update docs/prompts | Continuous optimization + quarterly recalibration |

### Last assessment (edit each quarter)

| Dimension | Score (1–5) | Notes |
|-----------|-------------|-------|
| Instructions & prompts | | |
| Context management | | |
| Agent autonomy | | |
| Security controls | | |
| Quality evaluation | | |
| Cost governance | | |
| Deployment safety | | |
| Learning loop | | |

**Quarter / date:** _YYYY-Qn_  
**Reviewers:** _names / roles_

## RACI (summary)

**A** = Accountable, **R** = Responsible, **C** = Consulted, **I** = Informed

| Activity | PM | Tech Lead | Security | AI Platform | Developers | QA / Release | Ops |
|----------|-----|-----------|----------|-------------|------------|--------------|-----|
| Approve feature mini-spec | A | R | C | C | C | I | I |
| Approve architecture / ADR | C | A/R | C | C | I | I | I |
| Maintain repo instructions & SDLC prompts | I | A | C | R | C | I | I |
| Approve MCP allow-list changes | I | C | A | R | I | I | C |
| Implement scoped change | I | C | I | C | R | C | I |
| Review AI-generated PR | I | A | C | C | R | C | I |
| Per-output eval on AI artifacts (story/code/prompt) | I | A | C | R | C | C | I |
| Run release readiness review | I | C | C | I | I | A/R | C |
| Approve production deployment | I | C | C | I | I | R | A |
| Postmortem & feed learning loop | C | A | C | R | C | C | R |

_Adjust roles to match your org titles; keep single **A** per row where possible._

## Rolling metrics (monthly or continuous)

Complement the quarterly scorecard with:

- **Per-output eval scores** — story, multilingual, code/test quality (see [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md))  
- **PR acceptance rate** — merged vs changes requested for `ai-assisted` PRs  
- **Re-prompt rate** — optional count per task  
- **Cost by task class** — per [COST_GOVERNANCE.md](COST_GOVERNANCE.md)  

## Success metrics (examples)

Track over time where tooling allows:

- **Delivery:** lead time, PR cycle time, deployment frequency, escaped defects  
- **AI effectiveness:** acceptance rate of AI-assisted PRs, re-prompt rate, test pass rate, **mean evaluation scores** by output type  
- **Security / governance:** secrets incidents, policy violations, unapproved MCP usage, rollback rate  
- **Cost:** cost per task class, cache hit rate, average context size (if measured)  
- **Product quality:** language QA, narration completion, moderation pass rate  

## Related repo artifacts

- [AGENTS.md](../AGENTS.md) — developer and agent workflow  
- [MCP_POLICY.md](MCP_POLICY.md) — MCP usage  
- [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md), [COST_GOVERNANCE.md](COST_GOVERNANCE.md), [CONTEXT_LIFECYCLE.md](CONTEXT_LIFECYCLE.md), [AGENT_EXECUTION_BOUNDARIES.md](AGENT_EXECUTION_BOUNDARIES.md), [TAMIXA_AI_CONTROL_PLANE.md](TAMIXA_AI_CONTROL_PLANE.md)  
- [docs/adr/README.md](adr/README.md), [docs/specs/README.md](specs/README.md) — decisions and mini-specs  
- [.github/copilot-instructions.md](../.github/copilot-instructions.md) — GitHub Copilot repo instructions  
