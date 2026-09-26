---
inclusion: always
---

# Professional Coding Standards

All code must meet production-grade quality suitable for code review. Follow these standards.

## Code Quality

1. **No silent failures** – Catch blocks must log and either rethrow or handle explicitly. Empty catch is forbidden.
2. **Defensive validation** – Validate inputs at API boundaries (controller/service). Use `@Valid`, size limits, allowlists.
3. **Structured logging** – Log at appropriate level: `info` for business events, `warn` for recoverable issues, `error` for failures. Include IDs (storyId, parentId) for traceability.
4. **Single responsibility** – Controllers handle HTTP; services hold logic; repositories handle persistence. Do not mix concerns.

## Error Handling

```kotlin
// ❌ BAD
} catch (e: Exception) {}

// ✅ GOOD
} catch (e: Exception) {
    log.error("Operation failed for id={}", id, e)
    throw ServiceException("Unable to complete", e)
}
```

## API Design

- Return consistent JSON shapes (PagedResponse, ErrorResponse).
- Use HTTP status codes correctly: 201 Created, 400 Bad Request, 401/403 for auth, 404 Not Found.
- Never expose stack traces or internal paths to clients in production errors.

## Testing

- Critical paths (auth, payments, story generation) must have integration or unit tests.
- Use Testcontainers for DB-dependent tests; mock external APIs.
