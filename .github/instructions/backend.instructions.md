---
applyTo: "backend/**"
---

# Backend (Spring Boot / Kotlin)

- Follow existing Spring Boot and Kotlin conventions; preserve **controllers → services → domain → adapters** boundaries.
- Add **unit tests** for business logic and **integration tests** for endpoints or persistence where the project already does so.
- Prefer **explicit DTOs** in `api/**/dto/`; do not leak JPA entities to REST responses.
- For queues or long-running workflows, document **state transitions** and **failure handling**.
- For **AI provider** changes: timeouts, retries, idempotency, and audit-friendly logging (no secrets or PII).
- Run backend tests: `./gradlew :backend:test -Ptamixa.backendOnly=true`
- **AI Control Plane** (in progress): `com.tamixa.controlplane` — ports only; schema `ai_*` (Flyway V75); see `docs/AI_GOVERNANCE_E2E.md` and `docs/CONTROL_PLANE_ARCHITECTURE.md`.
