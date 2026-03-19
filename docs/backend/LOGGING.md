# Logging Standards – Security & Compliance

## Overview

Tamixa backend logging follows security and compliance requirements for a children's app. This document defines standards for all application logging.

## Security Rules

### Never Log

- **Secrets**: Passwords, API keys, JWT tokens, refresh tokens, magic links
- **OTP codes**: Never log OTP or verification codes
- **PII**: Child names, full emails, full phone numbers
- **Sensitive identifiers**: Use IDs (parentId, storyId) for traceability; mask identifiers when needed

### Masking

Use `PiiMask` (`com.tamixa.infrastructure.logging.PiiMask`) for any PII in logs:

| Type   | Method           | Example output      |
|--------|------------------|---------------------|
| Email  | `maskEmail(s)`   | `ab***@domain.com`   |
| Phone  | `maskPhone(s)`   | `***1234`           |
| Token  | `maskToken(s)`   | `***xy12`           |
| Secret | `redact()`       | `***`               |

**Important: PiiMask is one-way.** It does not store or retain original values. You cannot "view" or unmask originals from logged output. To correlate with real users: use IDs (parentId, storyId, traceId) in logs, then look up in the database.

### Log Levels

| Level  | Use for                                                  |
|--------|----------------------------------------------------------|
| ERROR  | Failures requiring attention; include traceId, IDs       |
| WARN   | Recoverable issues (rate limits, retries, validation)    |
| INFO   | Business events (login, story generation, pipeline)      |
| DEBUG  | Developer diagnostics; disabled in production          |

### Structured Logging

- Use parameterized messages: `log.info("Story id={} status={}", id, status)` – never string concatenation for user input
- Include IDs for traceability: `storyId`, `parentId`, `traceId`
- Audit events use JSON structure via `AuditLogPort`

## Audit Logging

Security-sensitive events are logged through `AuditLogPort`:

- `logLoginAttempt` – Email login (success/failure)
- `logOtpLoginAttempt` – OTP login (success/failure, masked phone)
- `logStoryGeneration` – Story created for parent
- `logVoiceUpload` / `logVoiceDelete` – Family voice changes
- `logSubscriptionChange` – Billing events
- `logAdminAction` – Admin operations (masked admin email)
- `logDataExportRequest` – GDPR data export

Audit events are JSON-formatted and can be shipped to a SIEM or log aggregator.

## Children's App Specific

- **Content safety**: Moderation rejections logged with reason (no story content)
- **No child names in logs**: Use childId only
- **Billing**: `billing_audit_log` table for financial audit; logs reference parentId, subscriptionId

## Request Tracing

Every request has a `traceId` (X-Request-Id). It is set in MDC by `RequestTracingFilter` and included in error responses. Use it to correlate logs across services.

## Error Handling

- Catch blocks must log and rethrow or handle explicitly (no empty catch)
- Never expose stack traces or internal paths to clients in production
- `GlobalExceptionHandler` returns generic messages in production; `traceId` enables support lookup

## Feature Logging Coverage

Logging is implemented across all major features:

| Feature | Controllers | Services | Events Logged |
|---------|-------------|----------|---------------|
| Auth | AuthController | AuthService, OtpService | login, register, OTP send/verify (audit + PII masked) |
| Subscription | SubscriptionController | SubscriptionService | usage, upgrade, cancel |
| Children | ChildController | ChildService | create, list, update |
| Stories | StoryController, CuratedStoryController | StoryService | generate, list, search |
| Playback | PlaybackPositionController | PlaybackPositionService | save, get position, recent |
| Favorites | FavoriteStoryController | FavoriteStoryService | add, remove, list |
| Feedback | FeedbackController | FeedbackService | submit |
| Data Export | DataExportController | DataExportService | request, list |
| Admin | AdminController | AdminService | all admin actions (audit) |

## Compliance

- **GDPR**: No PII in logs; data export requests audited
- **Financial**: Billing events in `billing_audit_log`; subscription state changes in `subscription_events`
- **Admin**: All admin actions in `admin_audit` and `AuditLogPort`
