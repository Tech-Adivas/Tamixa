# API error responses

All REST error responses use a consistent JSON shape so clients can parse and display messages uniformly.

## Standard error body

Errors returned by the backend (including `GlobalExceptionHandler` and filters) use this shape:

```json
{
  "message": "Human-readable error message",
  "status": 400,
  "traceId": "optional-request-trace-id",
  "timestamp": "2025-03-15T12:00:00Z",
  "errors": { "fieldName": "Validation message" }
}
```

- **message** (string) — Always present; primary text to show the user.
- **status** (number) — HTTP status code (e.g. 400, 401, 403, 404, 409, 429, 500).
- **traceId** (string, optional) — Request trace ID for support/debugging.
- **timestamp** (string) — ISO-8601 time when the error was generated.
- **errors** (object, optional) — Present for validation failures (`400`); map of field name → validation message.

## HTTP status codes

| Code | Meaning |
|------|--------|
| 400 | Bad Request — validation failed or invalid input; check `errors` if present. |
| 401 | Unauthorized — missing or invalid token; re-login. |
| 403 | Forbidden — not allowed to perform this action. |
| 404 | Not Found — resource does not exist. |
| 409 | Conflict — e.g. bulk generation already in progress, duplicate resource. |
| 429 | Too Many Requests — rate limit exceeded; see below. |
| 500 | Internal Server Error — unexpected failure; use `traceId` when contacting support. |

## Rate limiting (429)

When the client exceeds rate limits:

- **Response**: HTTP `429 Too Many Requests` with the standard error body.
- **Retry-After** (header): Seconds to wait before retrying (e.g. `60`).
- **X-RateLimit-Limit** (on success): Max requests per window.
- **X-RateLimit-Remaining** (on success): Remaining requests in the current window.

Story-generation rate limits are stricter for free users; paid users have a higher quota. On 429, clients should wait `Retry-After` seconds and then retry once.

## OpenAPI / Swagger

The live Swagger UI at `/swagger-ui.html` documents request/response schemas. Error response schemas (message, status, traceId, timestamp, errors) are not always explicitly modeled in OpenAPI; treat the shape above as the contract for all error responses.
