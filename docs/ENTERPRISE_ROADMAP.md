# Enterprise roadmap: consumer quality now, B2B later

Tamixa is a **consumer-facing** kids’ stories product today, built to **enterprise-quality** standards. **B2B** (schools, districts, publishers) is **out of scope for implementation** until there is a **committed buyer**—then Phase 2 applies.

---

## Active scope (now)

| Status | Phase |
|--------|--------|
| **In progress** | **Phase 1 — Enterprise quality** (security, reliability, observability, hygiene). |
| **Deferred** | **Phase 2 — B2B readiness** — do not build org/SSO/RBAC expansion until a real B2B contract or pilot is signed. |

**Principle:** Ship value to parents with operable, secure systems. Keep domain boundaries clean so Phase 2 is additive later ([ARCHITECTURE.md](ARCHITECTURE.md)).

---

## Phase 1 — Execution checklist

Use this as a working backlog; check items off as you complete them in your environment.

### Security & privacy

- [ ] Production: strong `JWT_SECRET`, no default dev secret; `SPRING_PROFILES_ACTIVE=prod` (enforced by `JwtSecretValidator`)
- [ ] Production: `CORS_ALLOWED_ORIGINS` explicit (no `*`) — enforced by `CorsOriginValidator`
- [ ] Production: **`DEV_OTP_CODE` and `DEV_PASSWORDLESS_CODE` unset** — enforced at startup by `ProductionDevBypassValidator` (`backend/.../config/ProductionDevBypassValidator.kt`)
- [ ] Secrets only via env / secret manager; `.env` never committed ([SECURITY.md](SECURITY.md)); run `./scripts/verify-phase1-enterprise.sh` locally to catch tracked `.env`
- [ ] `SEED_ADMIN_ENABLED=false` and `dev` profile off in production ([DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md))
- [ ] OpenAI moderation policy matches risk tolerance (`OPENAI_MODERATION_REQUIRED` in prod as appropriate)
- [ ] Optional: [guardrails-service](AI_GUARDRAILS.md) enabled where you want structural JSON checks; `GUARDRAILS_SERVICE_FAIL_OPEN` set per availability needs
- [ ] Subprocessors / privacy docs current for support questions ([COMPLIANCE.md](COMPLIANCE.md))

### Reliability & data

- [ ] Automated DB backups enabled (managed provider or scheduled `pg_dump` to encrypted storage)
- [ ] **Tested restore** at least quarterly ([runbooks/BACKUP_AND_RESTORE.md](runbooks/BACKUP_AND_RESTORE.md))
- [ ] Load balancer / platform uses **trusted** `X-Forwarded-For` for rate limiting ([DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md))
- [ ] Kubernetes or PaaS: readiness uses `GET /actuator/health/readiness` (includes `db`, `redis`, `diskSpace`)

### Observability & incidents

- [ ] Metrics scraped (e.g. Prometheus `/actuator/prometheus`) and **alerting** on error rate / latency SLO burn
- [ ] Log aggregation with **trace id** correlation where enabled ([backend/LOGGING.md](backend/LOGGING.md))
- [ ] On-call knows [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) and runbook index ([runbooks/README.md](runbooks/README.md))

### Engineering hygiene

- [ ] **CI green** on default branch: backend tests, web build, admin build ([.github/workflows/ci.yml](../.github/workflows/ci.yml)); **`npm audit --audit-level=critical`** on web and admin; **Gradle wrapper** validated on each push/PR ([gradle-wrapper-validation.yml](../.github/workflows/gradle-wrapper-validation.yml))
- [ ] Before releases: `./scripts/verify-phase1-enterprise.sh` (backend compile + tests + `.env` not tracked)
- [ ] **Dependabot** enabled (`.github/dependabot.yml`) — review and merge security PRs promptly
- [ ] Flyway migrations backward-compatible per release process ([.cursor/rules/database-migrations-agent.mdc](../.cursor/rules/database-migrations-agent.mdc))

### Scale (when you measure the need)

- [ ] Narration/video latency or thread-pool saturation → design **durable queue** (managed or Kafka) before horizontal bottlenecks hurt SLOs ([ARCHITECTURE.md](ARCHITECTURE.md))

---

## Phase 1 — Initial SLO targets (draft)

Tune with real metrics; these are defaults to align ops and product.

| Journey | Target (starter) | Notes |
|---------|------------------|--------|
| **Auth** (login / refresh) | 99% success, p95 latency under 2s API-side | Exclude user typo; measure server errors/timeouts. |
| **Story generation** (submit → READY or clear failure) | 99% **technical** success over 24h | Business moderation rejections are not “SLO miss” if intentional. |
| **Playback** (stream URL) | 99.5% success for entitled requests | CDN/S3 errors belong in alerting. |
| **API availability** | 99.9% monthly (excluding planned maintenance) | Adjust for single-region MVP. |

Document your actual numbers and dashboards in your ops wiki when live.

---

## Phase 2 — B2B readiness (deferred)

**Do not implement** until there is a **real B2B buyer** (pilot or contract). Then revisit: tenant/org model, SSO (OIDC/SAML), expanded RBAC, institutional audit/export, contract billing.

High-level prep is only **non-breaking** design (ports for auth, avoid hard-coding a single global content namespace everywhere). No new B2B tables or SSO until sales commits.

---

## Documentation map

| Topic | Document |
|-------|----------|
| System map & integrations | [ARCHITECTURE.md](ARCHITECTURE.md) |
| AI safety | [AI_GUARDRAILS.md](AI_GUARDRAILS.md) |
| Env & services | [ENV_REFERENCE.md](ENV_REFERENCE.md), [.env.example](../.env.example) |
| Security | [SECURITY.md](SECURITY.md), [backend/SECURITY_CHECKLIST.md](backend/SECURITY_CHECKLIST.md) |
| Deploy | [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md), [PRODUCTION_UPGRADE.md](PRODUCTION_UPGRADE.md) |
| Backups | [runbooks/BACKUP_AND_RESTORE.md](runbooks/BACKUP_AND_RESTORE.md) |
| Product roadmap (features) | [ROADMAP.md](ROADMAP.md) |

---

## Review cadence

- **Phase 1 checklist:** monthly team review until items are routinely green.
- **Phase 2:** reopen this doc when the **first B2B pilot** is signed.

---

*Phase 2 explicitly deferred until a committed B2B buyer; Phase 1 is the active execution track.*
