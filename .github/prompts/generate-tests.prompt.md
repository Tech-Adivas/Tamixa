# Generate tests

**Goal:** Propose tests that match the current change and project conventions.

## Inputs

- Current diff or described behavior
- Nearest applicable instruction files (backend, mobile, testing agent rule)

## Instructions

Using the current diff and nearest instruction files:

- Propose **unit**, **integration**, and **edge-case** tests.
- Prioritize **regression** coverage for changed behavior.
- Keep tests **readable** and **deterministic** (no flaky time/network unless containerized/mocked).
- Explain any **gaps** that need human judgment or special environment setup.

## Tamixa conventions

- Backend: `./gradlew :backend:test -Ptamixa.backendOnly=true`; integration patterns per [.cursor/rules/testing-agent.mdc](../../.cursor/rules/testing-agent.mdc).
- Respect **no PII** in fixtures; **no real secrets**.
