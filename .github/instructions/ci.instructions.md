---
applyTo: ".github/workflows/**,.github/dependabot.yml"
---

# CI and automation

- **Triggers:** align with existing workflows (push/PR to `main`, `develop` unless explicitly changing policy).
- Backend job uses **Java 17**, Postgres and Redis services, and `./gradlew :backend:test -Ptamixa.backendOnly=true` — keep flags and env vars consistent with [ci.yml](../workflows/ci.yml).
- Web/Admin: **Node 20**, prefer `npm ci` with lockfiles; do not weaken `npm audit` thresholds without review.
- **Agents do not deploy to production** — CI verifies; human-approved release process applies for rollout ([docs/MCP_POLICY.md](../../docs/MCP_POLICY.md), [AGENTS.md](../../AGENTS.md)).
