---
applyTo: "**/*.yml,**/*.yaml,**/.github/workflows/**,**/infra/**,**/security/**"
---

# Security-sensitive paths

- Treat credentials, tokens, webhooks, and deployment permissions as **sensitive**; use **least privilege** and **short-lived** credentials.
- **Never** embed secrets in code, tests, examples, or logs; `.env` is gitignored — use `.env.example` with placeholders only.
- Call out **blast radius**, **rollback**, and **audit** impact in every plan.
- For **MCP** or agent tool changes, specify scopes, owners, telemetry, and kill-switch behavior; see [docs/MCP_POLICY.md](../../docs/MCP_POLICY.md).
