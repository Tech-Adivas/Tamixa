# Araro Project Guidelines for AI & Developers

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
4. **Naming conventions** – Per `.cursor/rules/naming-conventions.mdc` (backend, mobile, web, admin). See also `docs/NAMING_CONVENTIONS.md` for a standalone reference.

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
4. Run tests before committing: `./gradlew :backend:test -Pararo.backendOnly=true`
