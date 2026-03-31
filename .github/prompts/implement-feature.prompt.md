# Implement feature

**Goal:** Execute the smallest safe change set that matches an agreed mini-spec or ticket.

## Inputs

- Approved mini-spec (`docs/specs/` or issue) or clearly trivial task
- Nearest instruction files: [.github/instructions/](../instructions/), [.cursor/rules/](../../.cursor/rules/), [AGENTS.md](../../AGENTS.md)

## Required workflow

1. **Think** — restate task and risk level (low/medium/high).
2. **Research** — open only the files and tests you need.
3. **Plan** — implementation steps, tests, rollback (even if brief).
4. **Act** — code + tests + docs touchpoints; no scope creep.
5. **Verify** — run [evaluate-output.prompt.md](evaluate-output.prompt.md) or Review/CI for substantive AI output when applicable ([docs/AI_EVALUATION_SYSTEM.md](../../docs/AI_EVALUATION_SYSTEM.md)).
6. **Report** — what changed, how to verify, open risks.

## Guardrails

- Do **not** perform irreversible actions (production data, secret rotation) without explicit human approval.
- Do **not** edit deployment or secrets-only config without calling it out and getting approval.
- Prefer **sandboxed** commands and **deterministic** tests.
- If instructions conflict, prefer the **most specific** instruction file (e.g. `backend/**` over repo root).

## Output

- Concise summary, file list, and exact commands to run (e.g. `./gradlew :backend:test -Ptamixa.backendOnly=true`, `npm test` in `admin/` or `web/`).
