# AI usage metrics

This document defines the AI usage metrics shown in the admin dashboard and exposed by backend admin APIs.

## Scope

- **Dashboard page:** `admin/src/app/(dashboard)/dashboard/ai-metrics/page.tsx`
- **Primary API:** `GET /api/v1/admin/metrics/ai`
- **Controller:** `backend/src/main/kotlin/com/tamixa/api/controller/AdminController.kt`
- **Service logic:** `backend/src/main/kotlin/com/tamixa/application/admin/AdminService.kt`
- **DTO contract:** `backend/src/main/kotlin/com/tamixa/api/admin/dto/AiMetricsDto.kt`

## API response contract

`GET /api/v1/admin/metrics/ai` returns:

- `storyGenerationsTotal` (number)
- `cacheHits` (number)
- `cacheMisses` (number)
- `voiceProcessingCount` (number)
- `openaiTokensUsed` (number | null)
- `apiBreakdown` (array of `ApiUsageDto`)
- `storyBreakdown` (array of `StoryAiUsageDto`)

Type references:

- `admin/src/types/api.ts` (`AiMetricsDto`, `ApiUsageDto`, `StoryAiUsageDto`)
- `backend/src/main/kotlin/com/tamixa/api/admin/dto/AiMetricsDto.kt`

## Metric definitions

| Metric | Definition | Source |
|---|---|---|
| `storyGenerationsTotal` | Total completed story generation operations. | Micrometer timer `story_generation_latency` count |
| `cacheHits` | Story cache hit counter. | Micrometer counter `story.cache.hits` |
| `cacheMisses` | Story cache miss counter. | Micrometer counter `story.cache.misses` |
| `voiceProcessingCount` | Total voice processing operations. | Micrometer timer `voice.processing.latency` count |
| `openaiTokensUsed` | Combined token total from story + narration paths. | `story.openai.tokens.total` + `narration_ai_tokens_total` |

## Per-API breakdown (`apiBreakdown`)

The service currently returns these API keys:

- `openai_llm` -> `OpenAI (story/rewrite)`
- `google_tts` -> `Google Cloud TTS`
- `openai_tts` -> `OpenAI TTS`
- `heygen_avatar` -> `HeyGen (avatar video)`
- `replicate_avatar` -> `Replicate SadTalker (avatar)`
- `did_avatar` -> `D-ID (avatar)`
- `gooey_avatar` -> `Gooey.AI (avatar)`

Notes:

- `tokensOrCharacters` is populated where available.
- `costEstimateUsd` is currently `null` for per-API rows.
- For Google TTS characters, backend uses fallback order:
  1. `ai.google_tts.characters`
  2. `narration_tts_characters_total` (if the first metric is zero)

## Per-story breakdown (`storyBreakdown`)

`storyBreakdown` is built from:

- token totals by story (`StoryTokenUsageJpaRepository.sumTokensByStoryId`)
- avatar video counts by story (`StoryAvatarVideoJpaRepository.countByStoryId`)
- story titles (`StoryJpaRepository.findTitlesByIds`)

The list is sorted by `totalTokens` descending and limited to top 100 rows.

### Cost estimate formula (per story)

Current estimate in `AdminService`:

- `costUsd = (totalTokens / 1_000_000.0) * 0.001 + (avatarVideoCount * 0.10)`
- `costInr = costUsd * 93.0`

This is an operational approximation for dashboard visibility, not an invoicing source of truth.

## Access control

- Endpoint requires: `@adminAuth.hasPermission('VIEW_AI_METRICS')`
- Permission enum: `backend/src/main/kotlin/com/tamixa/domain/AdminPermission.kt`
- Role mapping: `backend/src/main/kotlin/com/tamixa/domain/RolePermissions.kt`

At present, `VIEW_AI_METRICS` is granted to:

- `SUPER_ADMIN`
- `ADMIN`
- `REVENUE_ANALYST`

## Refresh and UX behavior

Admin UI behavior in `ai-metrics/page.tsx`:

- Auto-refresh every 15 seconds (`AUTO_REFRESH_MS = 15_000`) when tab is visible.
- Manual refresh button.
- Relative "Updated Xs/m/h ago" indicator.
- If authorization fails, page shows permission-specific error message.

## Operational checklist

If AI metrics look empty or stale:

1. Verify admin role has `VIEW_AI_METRICS`.
2. Verify backend is emitting Micrometer meters listed above.
3. Verify recent AI flows executed (story generation / narration / avatar).
4. Verify DB-backed story usage tables contain recent data:
   - `story_token_usage` (via `StoryTokenUsageEntity`)
   - `story_avatar_videos` (via `StoryAvatarVideoEntity`)
5. Confirm backend endpoint directly:
   - `GET /api/v1/admin/metrics/ai` with a valid admin JWT.

## QA: API verification with curl

Use these examples to validate backend metrics independent of the admin UI.

### 1) Fetch AI metrics

```bash
curl -sS \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Accept: application/json" \
  "${API_BASE_URL}/api/v1/admin/metrics/ai"
```

Expected JSON shape:

```json
{
  "storyGenerationsTotal": 1234,
  "cacheHits": 789,
  "cacheMisses": 210,
  "voiceProcessingCount": 456,
  "openaiTokensUsed": 1200000,
  "apiBreakdown": [
    {
      "api": "openai_llm",
      "displayName": "OpenAI (story/rewrite)",
      "requests": 1234,
      "tokensOrCharacters": 1200000,
      "costEstimateUsd": null
    }
  ],
  "storyBreakdown": [
    {
      "storyId": 101,
      "title": "Sample title",
      "totalTokens": 8200,
      "avatarVideoCount": 1,
      "costInr": 9.31
    }
  ]
}
```

### 2) Validate authorization behavior

Without a role that has `VIEW_AI_METRICS`, the endpoint should return `403 Forbidden`.

```bash
curl -i \
  -H "Authorization: Bearer $NON_PRIVILEGED_JWT" \
  "${API_BASE_URL}/api/v1/admin/metrics/ai"
```

### 3) Compare API and dashboard quickly

- Open admin `AI usage metrics` page.
- Trigger `Refresh`.
- Call the curl endpoint within the same minute.
- Confirm top-level values match:
  - `storyGenerationsTotal`
  - `cacheHits`
  - `cacheMisses`
  - `voiceProcessingCount`
  - `openaiTokensUsed`

### 4) Fast smoke check (jq)

Print just the core KPI fields:

```bash
curl -sS \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Accept: application/json" \
  "${API_BASE_URL}/api/v1/admin/metrics/ai" \
  | jq '{
      storyGenerationsTotal,
      cacheHits,
      cacheMisses,
      voiceProcessingCount,
      openaiTokensUsed
    }'
```

Fail CI/local check when any KPI field is missing:

```bash
curl -sS \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Accept: application/json" \
  "${API_BASE_URL}/api/v1/admin/metrics/ai" \
  | jq -e '
      has("storyGenerationsTotal") and
      has("cacheHits") and
      has("cacheMisses") and
      has("voiceProcessingCount") and
      has("openaiTokensUsed")
    ' >/dev/null
```

### 5) Run via existing project verification script

The project script `scripts/verify-phase1-enterprise.sh` now includes an optional AI metrics smoke check.

```bash
API_BASE_URL="http://localhost:8080" \
ADMIN_JWT="<admin-jwt>" \
./scripts/verify-phase1-enterprise.sh
```

If `API_BASE_URL` or `ADMIN_JWT` is not set, the AI metrics check is skipped and the rest of the script still runs.

## Related docs

- `docs/AI_EVALUATION_SYSTEM.md`
- `docs/AI_GOVERNANCE_E2E.md`
- `docs/TAMIXA_AI_CONTROL_PLANE.md`
- `docs/METRICS_EXPORT.md`
