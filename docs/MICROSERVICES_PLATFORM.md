# Optional platform-style deployment (separate concern)

**This repository (araro-kids)** is a **standalone** Tamixa application monorepo. It **does not** depend on any other Tamixa Git repository to build, test, or run. There are **no** submodules, sync scripts, or Gradle/npm references tying it to another repo.

Some teams also operate a **separate** repository that hosts an edge **gateway**, extracted **notifications** service, and a **full-stack Compose** layout. That layout is **optional** and **orthogonal** to this repo: if you use it, you deploy and version it on its own. Align API contracts and env vars through documentation and integration tests—not through automated cross-repo pulls.

## What to run **here** (this repo)

| Goal | Where |
|------|-------|
| Local backend + Postgres + Redis + Kafka + optional guardrails | Root **`docker-compose.yml`** in **this** repo |
| Mobile, web, admin | As documented in repo **README** / **GETTING_STARTED** |

## Optional: remote notifications HTTP

If you delegate magic-link email to an **external** notifications HTTP service (any deployment that implements the same internal API contract), configure **`NOTIFICATIONS_EMAIL_REMOTE_*`** and **`NOTIFICATIONS_INTERNAL_API_KEY`** on the API per **`docs/ENV_REFERENCE.md`**. That service is **not** required to live in a specific named repository.

## Related

- Root **`.env.example`**, **`docs/ENV_REFERENCE.md`**
