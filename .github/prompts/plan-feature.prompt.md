# Plan feature (mini-spec before coding)

**Goal:** Produce a mini-spec for a non-trivial change before implementation.

## Inputs

- Ticket or feature request
- Relevant files and modules (backend, mobile, admin, web)
- Existing ADRs under `docs/adr/` or specs under `docs/specs/`

## Return (structured)

1. **Objective** — one paragraph
2. **In scope / out of scope**
3. **Impacted modules** — paths and layers (controllers, services, migrations, UI)
4. **Architecture / data impact** — migrations, API contracts, caching, jobs
5. **Risks and assumptions**
6. **Test plan** — unit, integration, manual checks
7. **Rollout and rollback**
8. **Questions for human review** — especially if touching auth, payments, voice consent, schema, or deployment

## Tamixa reminder

**Spec mode is mandatory** for schema, authentication, payments, voice consent, provider integrations, and deployment behavior. See [docs/specs/README.md](../../docs/specs/README.md).
