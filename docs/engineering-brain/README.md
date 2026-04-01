# Tamixa Engineering Brain (persistent context)

The **Engineering Brain** is the durable layer of **decisions, lessons, and constraints** that agents and humans should consult **before** relying on chat memory or a single long thread.

## What lives here (in-repo)

| Content | Location | Owner |
|---------|----------|-------|
| Architecture decisions | [docs/adr/](../adr/README.md) | Tech lead |
| Feature mini-specs | [docs/specs/](../specs/README.md) | PM + tech lead |
| Post-incident lessons | `LESSONS_LEARNED.md` (this folder) | Tech lead + Security |
| “Best prompts” pointers | Links to `.github/prompts/` and versioned code | AI platform / dev |
| Control plane source of truth | DB: `ai_*` tables (Flyway V75); E2E map: [AI_GOVERNANCE_E2E.md](../AI_GOVERNANCE_E2E.md) | Tech lead |
| Hard constraints | “Must not” list referencing [AGENT_EXECUTION_BOUNDARIES.md](../AGENT_EXECUTION_BOUNDARIES.md) | Tech lead |

## Optional file: lessons learned

Create **`LESSONS_LEARNED.md`** when you want a running log (short entries, dated). Prefer **ADRs** for decisions and **specs** for feature truth; use lessons for **failures**, **near misses**, and **eval surprises**.

## Query order for agents

1. Applicable **spec** and **ADR**  
2. **Context packs** for locale ([docs/context-packs/](../context-packs/README.md))  
3. **AGENTS.md** and path-specific rules  
4. Code and tests  

## Future

Replace or supplement this folder with an **internal searchable store** (read-only MCP, redacted logs). Until then, **git is the brain**.

## Related

- [TAMIXA_AI_CONTROL_PLANE.md](../TAMIXA_AI_CONTROL_PLANE.md)  
- [CONTEXT_LIFECYCLE.md](../CONTEXT_LIFECYCLE.md)  
