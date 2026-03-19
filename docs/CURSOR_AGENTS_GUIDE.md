# How to Use Cursor Agents in This Application

This guide explains what the “agents” are, how to turn them on, and how to get the best results with **sample prompts** you can copy and adapt.

---

## What Are These Agents?

The “agents” are **Cursor rules** (`.mdc` files in `.cursor/rules/`). Each one encodes a specific **role and checklist** so the AI follows project conventions when you ask it to do review, testing, API design, migrations, etc.

- **Always-on rules** (e.g. `professional-standards.mdc`, `security-vulnerabilities.mdc`) apply to every chat automatically.
- **Agent rules** (e.g. `review-agent.mdc`, `testing-agent.mdc`) are **optional**. You enable them when you want that behavior.

You don’t run a separate “agent app”—you use Cursor’s chat or Composer and either **enable the rule** or **tell the AI to act as that agent** in your prompt.

---

## Three Ways to Use an Agent

### 1. Rule Picker (before or during chat)

- Open the **rules** / **rule picker** in Cursor (e.g. via the rules dropdown or settings for the chat).
- Enable the rule that matches your task (e.g. **Review**, **Testing**, **API Design**).
- Then type your request. The AI will have that rule’s instructions in context.

**When to use:** When you’re about to do a focused task (e.g. “I’m going to review this PR” or “I’m adding a new endpoint”).

---

### 2. Mention the agent in your prompt

You don’t have to use the rule picker. You can **name the agent** in the first line of your message so the AI adopts that role:

- *“Use the **Review** agent: review this PR and list blockers and suggestions.”*
- *“Act as the **Testing** agent: add integration tests for the new subscription endpoint.”*

If the rule isn’t enabled, the AI may still follow the spirit of the agent if it has seen `AGENTS.md` or the rule file; for best results, **enable the rule** when you mention the agent.

---

### 3. Always-apply rules

Some rules (e.g. security, professional standards, naming) are set to `alwaysApply: true`. They’re always in context. The **specialized agents** (Review, Testing, Delivery, Security, Prompt/Story, Refactoring, Documentation, API Design, Database/Migrations, Observability) are **off by default** so you can turn them on only when needed.

---

## Agent-by-Agent Summary and Sample Prompts

Use the table below to choose an agent, then copy or adapt the sample prompts.

| Agent | When to use it | Rule file |
|-------|----------------|-----------|
| **Review** | Code review, PR feedback, merge checklist | `review-agent.mdc` |
| **Testing** | Adding tests, running tests, test patterns | `testing-agent.mdc` |
| **Delivery** | CI/CD, release, deployment | `delivery-agent.mdc` |
| **Security** | Security review, audit, vulnerability checks | `security-agent.mdc` |
| **Prompt / Story** | Story/narration prompts, Tamixa, safety | `prompt-story-agent.mdc` |
| **Refactoring** | Refactors, tech debt, in-place changes | `refactoring-agent.mdc` |
| **Documentation** | README, API docs, runbooks, .env.example | `documentation-agent.mdc` |
| **API Design** | New/changed REST, DTOs, OpenAPI | `api-design-agent.mdc` |
| **Database / Migrations** | Flyway, schema, backward compatibility | `database-migrations-agent.mdc` |
| **Observability** | Logging, metrics, actuator, tracing | `observability-agent.mdc` |
| **Code Validation** | Validate a file, its references, project rules | `code-validation-agent.mdc` |
| **Figma / Design** | Verify UI matches Figma or design spec | `figma-design-agent.mdc` |

---

### Review Agent

**When:** You want PR-style feedback, merge checklist, or consistent review comments.

**Sample prompts:**

1. **Full PR review**  
   *“Use the Review agent. Review the changes in this PR. Go through your checklist (secrets, validation, error handling, auth, dev-only, tests, naming). List any blockers and then concrete suggestions with file/line references where possible.”*

2. **Focused review**  
   *“Act as the Review agent. Check only security and authorization: are there any endpoints that should have @PreAuthorize or ownership checks missing? Cite file and line.”*

3. **Pre-merge checklist**  
   *“Run the Review agent’s pre-merge checklist on the current diff and tell me pass/fail for each item with a one-line reason.”*

---

### Testing Agent

**When:** Adding or updating tests, or figuring out how to run tests.

**Sample prompts:**

1. **Add tests for a new endpoint**  
   *“Use the Testing agent. I added a new POST /subscriptions/upgrade endpoint in SubscriptionController. Add integration tests that cover: valid request (201), validation failure (400), and unauthenticated (401). Use the project’s test patterns and run command.”*

2. **Run and fix tests**  
   *“Act as the Testing agent. Run the backend tests with the project command and fix any failures. Prefer Testcontainers for DB; mock external APIs.”*

3. **Test strategy**  
   *“Following the Testing agent: what should we test for the new StoryPromptBuilder.buildUserPrompt() method? List scenarios and whether each should be unit or integration.”*

---

### Delivery Agent

**When:** Changing CI/CD, preparing a release, or writing a deployment runbook.

**Sample prompts:**

1. **Add a new CI job**  
   *“Use the Delivery agent. We need a CI job that runs the mobile build (or lint) on every PR to main and develop. Add it to .github/workflows/ci.yml following the same style as the existing backend and admin jobs.”*

2. **Pre-release checklist**  
   *“Act as the Delivery agent. We’re about to cut a release from develop. Walk through your pre-release checklist and tell me what to verify (CI, env, migrations, feature flags) and what’s missing if anything.”*

3. **Deployment steps**  
   *“Following the Delivery agent: document the steps to deploy the backend to production (assume we use the existing Dockerfile and env vars). Keep it short and actionable.”*

---

### Security Agent

**When:** Security-focused review, audit, or hardening.

**Sample prompts:**

1. **Security pass on a feature**  
   *“Use the Security agent. Do a security pass on the new voice upload flow: secrets, input validation, auth, rate limiting, and PII in logs. List findings with rule violated, risk, and suggested fix.”*

2. **Secrets and env**  
   *“Act as the Security agent. Scan the backend and admin for hardcoded secrets, credentials in logs, or missing env vars. Report only concrete findings with file/location.”*

3. **Auth and ownership**  
   *“Following the Security agent: verify that every admin endpoint has @PreAuthorize and that parent/child and story access always checks resource ownership. List any gap with file and line.”*

---

### Prompt / Story Agent

**When:** Changing story generation or narration prompts (StoryPromptBuilder, system/user prompts, or docs).

**Sample prompts:**

1. **Add a new language hint**  
   *“Use the Prompt/Story agent. We want to add a cultural hint for Hindi (hi) in StoryPromptBuilder, similar to the Tamil one. Add it in code and update docs/backend/StoryPromptExamples.md. Keep safety and JSON shape unchanged.”*

2. **Tighten safety wording**  
   *“Act as the Prompt/Story agent. Review the system message in StoryPromptBuilder.buildSystemMessage() and suggest stricter wording for the ‘Banned’ list so the model never outputs violence or weapons. No PII in prompts.”*

3. **Sync docs and code**  
   *“Following the Prompt/Story agent: StoryPromptBuilder was changed to add a new field to the JSON. Update docs/backend/StoryPromptExamples.md so the expected JSON and examples match the code.”*

---

### Refactoring Agent

**When:** Refactoring, paying down tech debt, or moving code without changing behavior.

**Sample prompts:**

1. **Extract a service**  
   *“Use the Refactoring agent. Extract the subscription upgrade logic from SubscriptionController into a SubscriptionUpgradeService. Keep behavior identical. Add/update tests first, then refactor in small steps. Run backend tests after.”*

2. **In-place cleanup**  
   *“Act as the Refactoring agent. Clean up CuratedStoryService: remove dead code and duplicate logic, keep the same public API and behavior. One small step at a time, run tests after each step.”*

3. **Plan a refactor**  
   *“Following the Refactoring agent: I need to move story generation from Controller X to a new StoryGenerationService. Give me a short step-by-step plan (tests first, then move, then wire) without writing code yet.”*

---

### Documentation Agent

**When:** Writing or updating README, API docs, runbooks, or .env.example.

**Sample prompts:**

1. **Update README**  
   *“Use the Documentation agent. Update the project root README: add how to run backend (with .env), admin, and web; list main env vars (point to .env.example); keep it short and actionable.”*

2. **Document an endpoint**  
   *“Act as the Documentation agent. Document the new GET /curated-stories endpoint: purpose, query params, response shape, and example. Put it in docs/backend/ or next to the API, and link to the controller or OpenAPI.”*

3. **Env template**  
   *“Following the Documentation agent: add the new STRIPE_WEBHOOK_SECRET and OPENAI_API_KEY to .env.example with one-line descriptions. No real values.”*

---

### API Design Agent

**When:** Adding or changing REST endpoints, DTOs, or OpenAPI.

**Sample prompts:**

1. **Design a new endpoint**  
   *“Use the API Design agent. We need an endpoint to list a parent’s children with optional filters. Propose: HTTP method and path, request (query/body), response shape (use existing PagedResponse if applicable), status codes, and DTO placement. Follow project naming and validation.”*

2. **Fix response shape**  
   *“Act as the API Design agent. Our new endpoint returns a custom wrapper; the rest of the API uses ErrorResponse and PagedResponse. Change it to use the standard response shapes and correct status codes (201 for create, 400 for validation).”*

3. **OpenAPI and validation**  
   *“Following the API Design agent: add Bean Validation to CreateChildRequest and document the new POST /children endpoint in OpenAPI (summary, request/response, 201/400).”*

---

### Database / Migrations Agent

**When:** Adding or changing Flyway migrations or schema.

**Sample prompts:**

1. **New migration**  
   *“Use the Database/Migrations agent. Add a Flyway migration that adds a nullable column referral_code VARCHAR(50) to the parents table and an index on it. Use the project’s naming and location (db/migration, next version number).”*

2. **Safe drop**  
   *“Act as the Database/Migrations agent. We want to drop the column old_theme from stories. Propose a two-phase approach: first migration and code change, then cleanup migration, so we don’t break running code.”*

3. **Review a migration**  
   *“Following the Database/Migrations agent: review migration V45__add_foo.sql for backward compatibility, indexing, and naming. List any issues and suggest changes.”*

---

### Observability Agent

**When:** Adding or changing logging, metrics, or health checks.

**Sample prompts:**

1. **Add metrics for a new flow**  
   *“Use the Observability agent. We have a new ‘story translation’ flow. Add metrics to ApplicationMetrics: a timer for translation latency and a counter for translation failures. Use the same naming style (dot-separated, lowercase) and register them in ApplicationMetrics.”*

2. **Logging review**  
   *“Act as the Observability agent. Review SubscriptionService: ensure important business events are logged at info, errors at error with context, and no secrets or PII in any log line. Suggest concrete log lines or fixes.”*

3. **Health and actuator**  
   *“Following the Observability agent: we added an external ‘content moderation’ service. Document how to add a custom health indicator for it and expose it via /actuator/health without leaking internal URLs or keys.”*

---

### Code Validation Agent

**When:** You want to validate a specific file and its references (imports, callers, callees) and get verification steps.

**Sample prompts:**

1. **Validate one file and references**  
   *“Use the Code Validation agent. Validate the file I have open (SubscriptionController.kt): check imports, types, who calls it and what it calls, project naming and layer rules. List any issues with file:line and suggest verification steps (build, test, manual).”*

2. **After a rename or move**  
   *“Act as the Code Validation agent. I renamed StoryPromptBuilder and moved it to a new package. Find all references (imports, usages) that need to be updated and list them. Then suggest build/test steps to confirm nothing is broken.”*

3. **Validate my changes**  
   *“Use the Code Validation agent on my current changes. For each modified file: check references in and out, alignment with naming and structure, and give me a short verify list (build, run tests, anything to manually check).”*

---

### Figma / Design Agent

**When:** You want to verify that UI code (mobile, admin, web) matches Figma or the design spec.

**Sample prompts:**

1. **Check screen against design spec**  
   *“Use the Figma/Design agent. Compare the Welcome/Login screen implementation (mobile or admin) to docs/design/TAMIXA_UI_DESIGN_SPEC.md. List any deviations (colors, typography, spacing, button style) and suggest code changes. Also give me a short checklist I can use while comparing with Figma.”*

2. **Align component with design**  
   *“Act as the Figma/Design agent. Our primary CTA button should match the spec: Soft Gold #F6C453, rounded, soft glow. Check the button component(s) in the codebase and suggest exact changes (use design tokens if we have them). Then list what to verify in Figma (padding, radius, shadow).”*

3. **Full screen design pass**  
   *“Following the Figma/Design agent: verify the Dashboard screen (mobile Compose) against the design spec. Check colors, typography, spacing, and component styles. Output: (1) spec alignment with file:line for deviations, (2) a Figma checklist for me to run through, (3) suggested code fixes.”*

---

## Tips for Best Results

1. **Enable the rule** when you mention an agent—use the rule picker to turn on the right `.cursor/rules/*.mdc` so the AI has the full checklist.
2. **Be specific**—e.g. “review **this PR**”, “add tests for **POST /subscriptions/upgrade**”, “add a migration for **referral_code on parents**”.
3. **One agent per task**—for mixed tasks (e.g. “add endpoint and tests”), you can say: “Use API Design agent for the endpoint, then Testing agent for the tests.”
4. **Run commands yourself when needed**—e.g. “Run `./gradlew :backend:test -Ptamixa.backendOnly=true` and fix any failures” so the Testing agent’s fixes are validated.
5. **Reference files**—e.g. “In `StoryPromptBuilder.kt`…” or “See `AGENTS.md`” so the AI knows exactly what to change or follow.

---

## Quick Reference: Rule Files

All agents live under `.cursor/rules/`:

- `review-agent.mdc`
- `testing-agent.mdc`
- `delivery-agent.mdc`
- `security-agent.mdc`
- `prompt-story-agent.mdc`
- `refactoring-agent.mdc`
- `documentation-agent.mdc`
- `api-design-agent.mdc`
- `database-migrations-agent.mdc`
- `observability-agent.mdc`
- `code-validation-agent.mdc`
- `figma-design-agent.mdc`

The full list and when to use each are also in [AGENTS.md](../AGENTS.md) in the **Specialized Agents** table.
