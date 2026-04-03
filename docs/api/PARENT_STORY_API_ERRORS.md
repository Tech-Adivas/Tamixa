# Parent API: structured error `code` (stories)

JSON error bodies from the Spring API often include:

- `message` — human-readable text
- `status` — HTTP status (duplicate of the response status in many handlers)
- `traceId` — request correlation when tracing is enabled
- `timestamp` — ISO-8601 instant
- **`code`** (optional) — stable machine-readable string for clients

## `POST /api/v1/stories/generate`

Validation failures return **400** with a **`code`** so web/mobile clients can branch without parsing `message`:

| `code` | Typical `message` (example) |
|--------|-----------------------------|
| `UNKNOWN_GENERATION_TOPIC` | Unknown generationTopicId |
| `THEME_OR_TOPIC_REQUIRED` | Provide theme or generationTopicId |
| `GENERATION_LANGUAGE_NOT_SUPPORTED` | AI story generation is currently Tamil only |

Kotlin source: `com.tamixa.api.exception.ApiBadRequestException`, `ApiErrorCodes`, `StoryController.generate`.

Web client: `web/src/lib/api.ts` — `STORY_GENERATE_ERROR_CODES`, `ApiClientError`, `parseApiClientError`.

Mobile (KMP): `StoryApi.generate` maps HTTP **400** to `StoryGenerateBadRequestException` with `apiCode`; `errorMessageForUser` maps the three codes above to localized `Strings` (Tamil / Hindi / English).

## Other endpoints

Most **400** responses still use `IllegalArgumentException` handling and may omit **`code`**; only the generate path above is guaranteed to set these codes today.
