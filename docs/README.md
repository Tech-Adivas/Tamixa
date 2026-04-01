# Tamixa documentation

Central place for architecture, deployment, security, and feature documentation.

## Conventions and code quality

- **[NAMING_CONVENTIONS.md](NAMING_CONVENTIONS.md)** — Backend, mobile, web, and admin naming (DTOs, routes, files, env vars).
- **[NAMING_AUDIT.md](NAMING_AUDIT.md)** — Audit results: what follows conventions and what was fixed (e.g. API path base, Tamixa TTS naming).
- **[CODE_REVIEW_REPORT.md](CODE_REVIEW_REPORT.md)** — Full review with ✅ practices and ⚠️ follow-ups (inline DTOs, empty catches, etc.).

## Quick links

| Area | Documents |
|------|-----------|
| **Project overview (diagrams: context, layers, use cases, sequences, ER)** | [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) |
| **Agentic SDLC & AI governance** | [AGENTIC_SDLC_IMPLEMENTATION.md](AGENTIC_SDLC_IMPLEMENTATION.md), [AGENTIC_SDLC_GOVERNANCE.md](AGENTIC_SDLC_GOVERNANCE.md), [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md), [AI_GOVERNANCE_E2E.md](AI_GOVERNANCE_E2E.md), [AI_USAGE_METRICS.md](AI_USAGE_METRICS.md), [COST_GOVERNANCE.md](COST_GOVERNANCE.md), [CONTEXT_LIFECYCLE.md](CONTEXT_LIFECYCLE.md), [AGENT_EXECUTION_BOUNDARIES.md](AGENT_EXECUTION_BOUNDARIES.md), [TAMIXA_AI_CONTROL_PLANE.md](TAMIXA_AI_CONTROL_PLANE.md), [CONTROL_PLANE_ARCHITECTURE.md](CONTROL_PLANE_ARCHITECTURE.md), [MCP_POLICY.md](MCP_POLICY.md), [METRICS_EXPORT.md](METRICS_EXPORT.md), [engineering-brain/README.md](engineering-brain/README.md), [context-packs/README.md](context-packs/README.md) |
| **Architecture (full map + integrations)** | [ARCHITECTURE.md](ARCHITECTURE.md) |
| **Enterprise: quality now, B2B later** | [ENTERPRISE_ROADMAP.md](ENTERPRISE_ROADMAP.md) |
| **Getting started** | [GETTING_STARTED.md](GETTING_STARTED.md), [NAMING_CONVENTIONS.md](NAMING_CONVENTIONS.md), [FEATURES_IMPLEMENTED.md](FEATURES_IMPLEMENTED.md), [ROADMAP.md](ROADMAP.md) |
| **Security & compliance** | [SECURITY.md](SECURITY.md), [COMPLIANCE.md](COMPLIANCE.md), [PCI_SCOPE.md](PCI_SCOPE.md), [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) |
| **Deployment** | [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md), [DEPLOYMENT_PLAN_3_DAYS.md](DEPLOYMENT_PLAN_3_DAYS.md), [PRODUCTION_UPGRADE.md](PRODUCTION_UPGRADE.md) |
| **Backend** | [backend/LOGGING.md](backend/LOGGING.md), [backend/NARRATION_ARCHITECTURE.md](backend/NARRATION_ARCHITECTURE.md), [backend/SECURITY_CHECKLIST.md](backend/SECURITY_CHECKLIST.md), [backend/StoryPromptExamples.md](backend/StoryPromptExamples.md) |
| **API & runbooks** | [API_ERROR_RESPONSES.md](API_ERROR_RESPONSES.md), [ENV_REFERENCE.md](ENV_REFERENCE.md), [runbooks/README.md](runbooks/README.md) |
| **Voice & narration** | [ELEVENLABS_INTEGRATION.md](ELEVENLABS_INTEGRATION.md), [VOICE_CLONING_ARCHITECTURE.md](VOICE_CLONING_ARCHITECTURE.md), [AVATAR_VIDEO.md](AVATAR_VIDEO.md), [INDIAN_LANGUAGE_TTS_OPTIONS.md](INDIAN_LANGUAGE_TTS_OPTIONS.md) |
| **Mobile** | [mobile/](mobile/) — FOLDER_STRUCTURE, IOS_TESTING, FIREBASE_SETUP, etc. |
| **Admin** | [admin/](admin/) — DESIGN_SYSTEM, DEPLOYMENT |
| **Design** | [design/TAMIXA_UI_DESIGN_SPEC.md](design/TAMIXA_UI_DESIGN_SPEC.md) |
| **Analytics & product** | [STORY_ANALYTICS.md](STORY_ANALYTICS.md), [REVENUE_ANALYTICS.md](REVENUE_ANALYTICS.md), [DORA_METRICS_FRAMEWORK.md](DORA_METRICS_FRAMEWORK.md), [MOBILE_PRODUCT_EVALUATION.md](MOBILE_PRODUCT_EVALUATION.md), [MOBILE_CAPABILITIES_AND_ROADMAP.md](MOBILE_CAPABILITIES_AND_ROADMAP.md) |

## Structure

- **Root** — Cross-cutting: security, deployment, compliance, roadmap, naming.
- **backend/** — Backend-specific: logging, narration, security checklist, story prompts.
- **mobile/** — Mobile app: folder structure, iOS, Firebase, offline.
- **admin/** — Admin dashboard: design system, deployment.
- **design/** — UI/UX specs and assets.
