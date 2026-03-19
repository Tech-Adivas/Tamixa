# Tamixa Project Guidelines for AI & Developers

This document guides AI agents and developers toward consistent, professional, and secure code.

## Project Overview

- **Mobile app** (KMP, Compose) – Primary product; kid + parent experience
- **Backend** (Kotlin, Spring Boot) – REST API, PostgreSQL, Redis, S3
- **Admin** (Next.js) – Dashboard for content ops
- **Web** (React, Vite) – Parent-facing web app

## Priorities

1. **Mobile-first** – Per `.cursor/rules/mobile-first.mdc`
2. **Professional quality** – Per `.cursor/rules/professional-standards.mdc`
3. **Security** – Per `.cursor/rules/security-vulnerabilities.mdc`
4. **Naming conventions** – Per `.cursor/rules/naming-conventions.mdc` (backend, mobile, web, admin). See also [docs/NAMING_CONVENTIONS.md](docs/NAMING_CONVENTIONS.md) for a standalone reference.

## Specialized Agents

Use these rules when performing the corresponding tasks (enable in rule picker or reference in prompts). **For a detailed guide with sample prompts, see [docs/CURSOR_AGENTS_GUIDE.md](docs/CURSOR_AGENTS_GUIDE.md).**

| Agent | Rule | Use when |
|-------|------|----------|
| **Review** | `.cursor/rules/review-agent.mdc` | Code review, PR feedback, merge checklist |
| **Testing** | `.cursor/rules/testing-agent.mdc` | Adding tests, running test commands, test patterns |
| **Delivery** | `.cursor/rules/delivery-agent.mdc` | CI/CD changes, release, deployment checklist |
| **Security** | `.cursor/rules/security-agent.mdc` | Security review, audit, vulnerability checks |
| **Prompt / Story** | `.cursor/rules/prompt-story-agent.mdc` | Story/narration prompts, Tamixa persona, safety, StoryPromptBuilder |
| **Refactoring** | `.cursor/rules/refactoring-agent.mdc` | Refactors, tech debt, in-place changes, incremental steps |
| **Documentation** | `.cursor/rules/documentation-agent.mdc` | README, API docs, runbooks, .env.example |
| **API Design** | `.cursor/rules/api-design-agent.mdc` | New/changed REST endpoints, DTOs, OpenAPI, status codes |
| **Database / Migrations** | `.cursor/rules/database-migrations-agent.mdc` | Flyway migrations, schema changes, backward compatibility |
| **Observability** | `.cursor/rules/observability-agent.mdc` | Logging, metrics (ApplicationMetrics), actuator, tracing |
| **Code Validation** | `.cursor/rules/code-validation-agent.mdc` | Validate a file, its references, and alignment with project rules |
| **Figma / Design** | `.cursor/rules/figma-design-agent.mdc` | Verify UI implementation matches Figma or design spec |

## Code Review Checklist

Before merging, verify:

- [ ] No secrets or PII in logs
- [ ] Input validation on new endpoints
- [ ] Error handling with logging (no silent catches)
- [ ] Authorization checks on protected resources
- [ ] Tests for critical paths
- [ ] No `@Profile("dev")` logic enabled in production

## Architecture Conventions

- **Backend**: Hexagonal-ish – controllers, services, ports (interfaces), adapters (implementations)
- **Mobile**: KMP shared → `commonMain`, `androidMain`, `iosMain`
- **Admin/Web**: Component-based; use existing UI tokens and themes

## When Making Changes

1. Refactor in place; avoid creating redundant files.
2. Preserve existing patterns (repository ports, DTO naming).
3. Add logging for new flows; never log secrets.
4. Run tests before committing: `./gradlew :backend:test -Ptamixa.backendOnly=true`
