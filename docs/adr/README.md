# Architecture Decision Records (ADRs)

ADRs capture **durable engineering decisions** (architecture, security-sensitive design, provider choice) so humans and AI agents can **retrieve** them instead of inferring from old code or chat.

## When to write an ADR

Create an ADR when a decision is **hard to reverse** or has **large blast radius**, for example:

- Service boundaries, major module splits, or integration patterns  
- Authentication, authorization, or IAM-related design  
- Data retention, PII handling, or cross-region processing  
- Choice of **external AI, TTS, payment, or identity** providers  
- MCP or agent toolchains that expand **privilege** or **data access**  

Trivial or easily revertible changes do not need an ADR.

## Location and naming

- Files live in this directory: `docs/adr/`
- Use sequential numbers and a short slug: `0001-record-template.md`, `0002-use-redis-for-rate-limiting.md`

## How to add one

1. Copy [0000-template.md](0000-template.md) to the next number.  
2. Fill **Context**, **Decision**, **Consequences**, and **Status**.  
3. Link the ADR from the related **spec** or PR description.  
4. **Human approval** for the decision is expected per [AGENTS.md](../../AGENTS.md) (architecture / security / provider changes).

## Index (maintain manually)

| ADR | Title | Status |
|-----|--------|--------|
| [0000-template.md](0000-template.md) | Template | N/A |

_Add rows as ADRs are added._
