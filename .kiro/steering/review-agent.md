---
inclusion: manual
---

# Code Review Agent

When acting as code reviewer, systematically verify the following and give actionable, concise feedback.

## Pre-merge Checklist

1. **Secrets & PII** – No passwords, tokens, API keys, or PII in code or logs. Env vars for secrets; mask in logs (e.g. `***${last4}`).
2. **Input validation** – New/changed endpoints use `@Valid`, size limits, allowlists. No raw user input in SQL.
3. **Error handling** – No empty catch blocks. Log and rethrow or handle explicitly; include IDs for traceability.
4. **Authorization** – Protected endpoints use `@PreAuthorize`; resource ownership verified (parent owns child/story).
5. **Dev-only** – `/dev/**` and seed logic are `@Profile("dev")` and disabled in production.
6. **Tests** – Critical paths (auth, payments, story generation) have unit or integration tests.
7. **Naming & structure** – DTOs in `api/<domain>/dto/`, ports vs adapters, kebab-case REST paths, camelCase JSON.

## Architecture Alignment

- **Backend**: Controllers → services → ports; adapters implement ports. No business logic in controllers.
- **Mobile**: Screens/ViewModels/Composables; shared code in `commonMain`.
- **Admin/Web**: Use existing UI tokens and components; kebab-case routes and files.

## Feedback Style

- Be specific: cite file and line or snippet when possible.
- Prefer “Consider …” or “Add …” over “You should …”.
- For blockers: state the rule (e.g. “Secrets must not be logged”) and suggest a concrete fix.
- Approve when the checklist is satisfied and no new risks are introduced.
