# Runbooks index

Operational runbooks and debugging guides. Use this index to find the right doc for the task.

| Topic | Document | Description |
|-------|----------|-------------|
| **Incident response** | [../INCIDENT_RESPONSE.md](../INCIDENT_RESPONSE.md) | How to respond to incidents, severity, communication. |
| **Pipeline & story review** | [../admin/PIPELINE_FLOW_AND_DEBUGGING.md](../admin/PIPELINE_FLOW_AND_DEBUGGING.md) | Translation/TTS pipeline flow, how to debug stuck or failed stories. |
| **Narration architecture** | [../backend/NARRATION_ARCHITECTURE.md](../backend/NARRATION_ARCHITECTURE.md) | Narration steps, rewrite, TTS providers. |
| **Admin story pipeline** | [../admin/STORY_PIPELINE_FLOW.md](../admin/STORY_PIPELINE_FLOW.md) | Admin submit-for-review and approval flow. |
| **Story → Audio → Avatar** | [../admin/STORY_AUDIO_AVATAR_FLOW.md](../admin/STORY_AUDIO_AVATAR_FLOW.md) | End-to-end flow, config matrix, enterprise practices. |
| **Avatar video** | [AVATAR_VIDEO_TROUBLESHOOTING.md](AVATAR_VIDEO_TROUBLESHOOTING.md) | Avatar failures, provider fallback, recovery. |
| **Logging** | [../backend/LOGGING.md](../backend/LOGGING.md) | Backend logging, levels, and what to grep for. |

## Quick actions

- **Clear or restart pipeline for a story** — See pipeline debugging doc; reset pipeline state via admin or DB if documented.
- **Check rate limits** — API returns `429` with `Retry-After` and optional `X-RateLimit-*` headers; see [../API_ERROR_RESPONSES.md](../API_ERROR_RESPONSES.md).
- **Verify dev/seed is off in production** — Ensure `dev` profile and `SEED_ADMIN_ENABLED` are not enabled in prod; see [../ENV_REFERENCE.md](../ENV_REFERENCE.md).
