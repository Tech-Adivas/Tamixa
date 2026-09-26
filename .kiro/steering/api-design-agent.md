---
inclusion: manual
---

# API Design Agent

When adding or changing REST endpoints, keep responses and DTOs consistent with the rest of the backend.

## Response Shapes

- **Paged lists**: Use the existing `PagedResponse` (or equivalent) with `content`, `page`, `size`, `totalElements`, etc. Do not invent a new paging shape.
- **Errors**: Use the project’s `ErrorResponse` (or global exception handler). Same JSON shape for 4xx/5xx; no stack traces or internal paths in production.
- **Single resource**: Return the domain DTO (e.g. `StoryResponse`, `CuratedStoryResponse`) with 200 OK or 201 Created and a `Location` header for creates when appropriate.

## HTTP Semantics

- **201 Created** – After creating a resource; include body and optionally `Location`.
- **200 OK** – Success for get/update/delete when a body is returned.
- **204 No Content** – Success with no body (e.g. some deletes).
- **400 Bad Request** – Validation failures, bad parameters; body explains errors.
- **401 Unauthorized** – Missing or invalid auth (e.g. JWT).
- **403 Forbidden** – Authenticated but not allowed (e.g. wrong role or resource ownership).
- **404 Not Found** – Resource does not exist or caller has no access.
- **409 Conflict** – Business rule conflict (e.g. duplicate, state conflict) when applicable.

## DTOs & Placement

- One DTO per file under `api/<domain>/dto/` (e.g. `CreateChildRequest`, `StoryResponse`). No inline request/response classes in controllers.
- **Naming**: `*Request` for request bodies, `*Response` or `*Dto` for responses; match naming-conventions.mdc.
- **Validation**: Use `@Valid` and Bean Validation on request DTOs (`@NotBlank`, `@Size`, `@NotNull`, allowlists for enums).

## Paths & Conventions

- **REST paths**: kebab-case (e.g. `/curated-stories`, `/stream-url`).
- **JSON**: camelCase for all public API fields; no `@JsonProperty("snake_case")` for internal REST (use snake_case only for external APIs like OpenAI if required).

## OpenAPI

- Keep SpringDoc/OpenAPI in sync: document new endpoints, request/response types, and auth (e.g. bearer). Use consistent tags and summaries so the generated spec stays usable.

## Security

- Protect endpoints with `@PreAuthorize` and correct roles. Verify resource ownership in the service layer before returning or modifying data.
