# Review security

**Goal:** Severity-ranked review of planned or generated changes.

## Review checklist

- **Auth and access control** — `@PreAuthorize`, resource ownership (parent/child/story), admin routes
- **Secret handling** — env-only secrets, nothing in logs, `.env.example` vs `.env`
- **Unsafe output handling** — user-controlled content, AI outputs persisted or executed
- **MCP / tool scope** — no expansion of privileged tools without policy; see [docs/MCP_POLICY.md](../../docs/MCP_POLICY.md)
- **Dependency / supply chain** — risky new deps, pinned versions where appropriate
- **Logging, audit, rollback** — traceability for security-sensitive operations; rollback path for migrations and feature flags

## Return format

1. **Severity-ranked findings** (blocker / high / medium / low)
2. **Required remediations** for blockers and highs
3. **Optional hardening** for medium/low

Cross-check with [.cursor/rules/security-vulnerabilities.mdc](../../.cursor/rules/security-vulnerabilities.mdc) and [.cursor/rules/security-agent.mdc](../../.cursor/rules/security-agent.mdc).
