# Cost governance (task → model → tier)

**Cost awareness** in the blueprint is **operationalized** here: map **task class** to **model tier**, track **per-task** usage where possible, and enforce **budget** discipline.

## Task → model → cost tier

Use the **cheapest model that meets the quality bar** for the task class. Revisit mapping **monthly**.

| Task class | Examples | Suggested tier | Notes |
|------------|----------|----------------|-------|
| **T0 – trivial** | Format, rename suggestions, lint fixes | Smallest / fastest | Tight `max_output_tokens` |
| **T1 – routine** | Test generation, PR summary, doc edits, **routine per-output eval** | Mid or small | Prefer cached system prefix |
| **T2 – complex** | Feature implementation, refactors across modules | Mid–top | Branch context; avoid huge attachments |
| **T3 – high-stakes** | Architecture, security review, **multilingual story sign-off**, incident root cause | Top reasoning | Human always in loop |

**Routing rule:** classify with [AGENT_EXECUTION_BOUNDARIES.md](AGENT_EXECUTION_BOUNDARIES.md) task table (THINK/RESEARCH/PLAN/ACT/VERIFY), then pick tier.

## Per-task cost tracking

| Stage | What to log (manual or automated) |
|-------|-----------------------------------|
| Request | Task class, model id, approx input/output tokens (if UI shows) |
| Outcome | Eval score, merge outcome, re-prompt count |
| Rollup | Weekly: cost by task class; monthly: adjust tier table |

Until a central **Model Router** exists ([TAMIXA_AI_CONTROL_PLANE.md](TAMIXA_AI_CONTROL_PLANE.md)), use **team norms** + provider dashboards.

## Budget enforcement

1. **Soft budget:** weekly token/cost estimate per squad; review in standup.  
2. **Hard budget:** cap premium-tier calls per day for non-production work (org policy).  
3. **Escalation:** T3 requires **reason** in PR or ticket (security, launch blocker, locale quality).  

## Optimization tactics (see also Control Plane)

- **Prompt caching:** stable prefixes first; variable content last ([CONTEXT_LIFECYCLE.md](CONTEXT_LIFECYCLE.md)).  
- **Context trimming:** smallest file set; summarize long threads into specs.  
- **Model switching:** VERIFY/eval on T1; only promote to T3 if scores fail.  
- **Batch:** offline jobs (bulk eval, backfill) use batch/flex APIs when available.  

## Related

- [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md) — quality gate before paying for rework  
- [AGENTIC_SDLC_GOVERNANCE.md](AGENTIC_SDLC_GOVERNANCE.md) — cost governance maturity row  
