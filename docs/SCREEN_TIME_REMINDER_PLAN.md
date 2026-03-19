# Screen-Time Reminder Feature — Implementation Plan

## Overview

**Feature:** Parent-configurable reminder when the child uses the phone (or Tamixa on iOS) for more than a configurable threshold (e.g. 30 minutes). Reminder message is customizable (e.g. "Time for homework!").

**Platform behavior:**
- **Android:** Cross-app usage tracking via `UsageStatsManager` (device-wide)
- **iOS:** Tamixa-only session tracking (no cross-app API without Apple entitlement)
- **Shared:** Common models, API, reminder UI

---

## Phase 1: Backend & Parent Configuration (Week 1)

### 1.1 Database

**New migration: `V29__screen_time_reminder.sql`**

```sql
-- Parent-configurable screen-time reminder per child
CREATE TABLE screen_time_reminder (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    threshold_minutes INT NOT NULL DEFAULT 30,
    reminder_message TEXT NOT NULL DEFAULT 'Time for a break!',
    notify_parent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_screen_time_reminder_child ON screen_time_reminder(child_id);
```

**Rationale:** One reminder config per child. Parent must own child (enforced in service). `notify_parent` = optional push/notification to parent's device when threshold hit.

### 1.2 Domain & Ports

| File | Purpose |
|------|---------|
| `domain/ScreenTimeReminder.kt` | Domain model |
| `application/port/ScreenTimeReminderRepositoryPort.kt` | Interface |
| `infrastructure/persistence/ScreenTimeReminderRepositoryAdapter.kt` | JPA impl |
| `infrastructure/persistence/ScreenTimeReminderEntity.kt` | JPA entity |

### 1.3 Service & API

| File | Purpose |
|------|---------|
| `application/reminder/ScreenTimeReminderService.kt` | CRUD, ownership checks |
| `api/reminder/ScreenTimeReminderController.kt` | REST endpoints |

**Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/children/{childId}/screen-time-reminder` | Get reminder config (parent owns child) |
| PUT | `/api/v1/children/{childId}/screen-time-reminder` | Create/update config |
| DELETE | `/api/v1/children/{childId}/screen-time-reminder` | Disable/remove |

**Request/Response DTO:**
```kotlin
// PUT body
data class ScreenTimeReminderRequest(
    @field:Min(5) @field:Max(180) val thresholdMinutes: Int = 30,
    @field:NotBlank @field:Size(max = 200) val reminderMessage: String,
    val notifyParent: Boolean = false
)
```

### 1.4 Web App (Parent UI)

- Add "Screen Time Reminder" section in child settings or profile (web app).
- Form: threshold (slider/dropdown 15–60 mins), message (text), notify parent (toggle).
- Call `PUT /children/{id}/screen-time-reminder` on save.

---

## Phase 2: Mobile — Common (Week 2)

### 2.1 Shared Models & API

| Location | Purpose |
|----------|---------|
| `commonMain/domain/ModelScreenTimeReminder.kt` | `ScreenTimeReminder` data class |
| `commonMain/network/ScreenTimeReminderApi.kt` | `getReminder(childId)`, `updateReminder(childId, ...)` |
| `commonMain/repository/ScreenTimeReminderRepository.kt` | Fetch/store config (optional local cache) |

### 2.2 Expect/Actual — Usage Time Tracking

| File | Purpose |
|------|---------|
| `commonMain/expect/ScreenTimeTracker.kt` | `expect` interface |
| `androidMain/.../ScreenTimeTracker.kt` | Android impl (UsageStatsManager) |
| `iosMain/.../ScreenTimeTracker.kt` | iOS impl (Tamixa-only session) |

**Interface (expect):**
```kotlin
expect class ScreenTimeTracker {
    suspend fun getForegroundTimeSeconds(): Long  // cumulative today or session
    fun isUsageAccessGranted(): Boolean          // Android only; iOS always true
    fun requestUsageAccess(activity: Any?)        // Opens settings; Android only
}
```

### 2.3 Reminder Dialog (Common Compose)

- `commonMain/ui/component/ScreenTimeReminderDialog.kt`
- Shows `reminderMessage` with "OK" / "Dismiss".
- Kid-friendly, non-intrusive design.
- Triggered when threshold exceeded (called from platform-specific logic).

### 2.4 Settings Integration

- Add "Screen Time Reminder" to `SettingsScreen` (or child profile area if child is selectable).
- Show current config (threshold, message) and link to parent web app for editing (or inline if parent flow exists in app).
- On child device: show "Grant usage access" prompt (Android) if not granted.

---

## Phase 3: Android — Cross-App Tracking (Week 2–3)

### 3.1 Permissions

- `AndroidManifest.xml`: No extra permission; "Usage access" is a special setting, not a runtime permission.
- `ACTION_USAGE_ACCESS_SETTINGS` intent to send user to Settings.

### 3.2 UsageStatsManager Integration

| File | Purpose |
|------|---------|
| `androidMain/.../AndroidScreenTimeTracker.kt` | Implement `ScreenTimeTracker` |

**Logic:**
1. Check `appOpsManager.checkOpNoThrow(OP_GET_USAGE_STATS)` for PACKAGE_NAME.
2. If not granted → show dialog "Tamixa needs usage access to remind you about breaks" → open settings.
3. Query `UsageStatsManager.queryUsageStats(INTERVAL_DAILY, startOfDay, now)`.
4. Sum `totalTimeInForeground` across all apps (or exclude system UI if desired).
5. Return seconds.

### 3.3 Background / Foreground Checks

- Option A: **Polling** — When app is in foreground, poll `getForegroundTimeSeconds()` every 1–2 minutes.
- Option B: **Foreground service** — Less ideal for battery; avoid unless required.
- **Recommendation:** Poll when app is resumed and every 60s while in foreground. If child leaves app, next resume will re-check (usage stats are cumulative for the day).

### 3.4 Threshold Check Flow

1. On app resume + periodic: fetch `ScreenTimeReminder` for current child (or default child).
2. If `enabled` and `getForegroundTimeSeconds() >= thresholdMinutes * 60`:
3. Show `ScreenTimeReminderDialog` with `reminderMessage`.
4. Optionally: call backend `POST /reminder/acknowledged` to trigger parent notification.
5. Cooldown: do not show again for same day (or for X minutes) — store in `DataStore` or similar.

---

## Phase 4: iOS — Tamixa-Only Tracking (Week 2–3)

### 4.1 Session Tracker

| File | Purpose |
|------|---------|
| `iosMain/.../IosScreenTimeTracker.kt` | Implement `ScreenTimeTracker` |

**Logic:**
- Track `sessionStartTime` when Tamixa becomes active.
- `getForegroundTimeSeconds()` = `now - sessionStartTime` while in Tamixa.
- Reset session when app goes to background for > 5 minutes (configurable).
- Persist session start in `UserDefaults` to survive short backgrounding.

### 4.2 Integration with Playback

- Reuse `StoryPlaybackTracker` / playback lifecycle: when user is on story screen or listening, session is active.
- Alternative: Use `UIApplication` lifecycle (didBecomeActive / willResignActive) for app-level session.

### 4.3 Same Reminder UI

- Use same `ScreenTimeReminderDialog` from common code.
- Threshold check: when Tamixa session time exceeds `thresholdMinutes`.

### 4.4 Fallback: Scheduled Reminders (Optional)

- If parent prefers time-based: "Remind at 4 PM" — implement separately as `ScheduledReminder` (different table/API).

---

## Phase 5: Polish & Notify Parent (Week 3–4)

### 5.1 Parent Notification (Optional)

- When `notify_parent` is true and child acknowledges (or threshold hit):
- Backend: `POST /reminder/triggered` from mobile with `childId`, `screenTimeSeconds`.
- Backend sends push to parent (FCM / APNs) — requires parent device token storage.
- Or: parent gets in-app notification when they next open web/mobile.

### 5.2 Cooldown & UX

- Cooldown: e.g. max 1 reminder per child per day (or per 2 hours).
- Store `last_reminder_shown_at` in local storage (DataStore / UserDefaults).
- Backend could also store `last_triggered_at` per child to avoid parent spam.

### 5.3 Strings & i18n

- Add `Strings.screenTimeReminder*`, `grantUsageAccess`, etc. for supported languages (ta, en, etc.).

---

## File Structure Summary

```
backend/
├── src/main/kotlin/com/tamixa/
│   ├── domain/ScreenTimeReminder.kt
│   ├── application/port/ScreenTimeReminderRepositoryPort.kt
│   ├── application/reminder/ScreenTimeReminderService.kt
│   ├── api/reminder/ScreenTimeReminderController.kt
│   ├── api/reminder/dto/
│   └── infrastructure/persistence/
│       ├── ScreenTimeReminderEntity.kt
│       └── ScreenTimeReminderRepositoryAdapter.kt
└── src/main/resources/db/migration/
    └── V29__screen_time_reminder.sql

mobile/
├── shared/src/commonMain/kotlin/com/tamixa/
│   ├── domain/ModelScreenTimeReminder.kt
│   ├── network/ScreenTimeReminderApi.kt
│   ├── ui/component/ScreenTimeReminderDialog.kt
│   └── expect/ScreenTimeTracker.kt          # expect class
├── composeApp/src/
│   ├── androidMain/.../AndroidScreenTimeTracker.kt
│   └── iosMain/.../IosScreenTimeTracker.kt

web/
└── src/
    ├── pages/... (child profile/settings — add reminder form)
    └── lib/api.ts (add screenTimeReminder endpoints)
```

---

## Dependencies

- **Android:** `UsageStatsManager` — no new Gradle deps.
- **iOS:** `UserDefaults`, `NotificationCenter` (app lifecycle) — no new Pods.
- **Backend:** No new deps.
- **Web:** Existing fetch/API patterns.

---

## Security & Privacy

- Reminder config: parent must own child (`child.parentId == parent.id`).
- Usage stats: **stored on-device only** for Android; never send raw per-app breakdown to backend.
- Optional: backend can store `lastTriggeredAt` + `screenTimeSeconds` (aggregate) for analytics — no PII.
- Do not log child names or reminder text.

---

## Testing

| Area | Approach |
|------|----------|
| Backend | Unit tests for `ScreenTimeReminderService` (ownership, validation). Integration test for controller. |
| Android | Unit test `AndroidScreenTimeTracker` with mocked `UsageStatsManager`. Manual: grant usage access, verify sum. |
| iOS | Unit test `IosScreenTimeTracker` session math. Manual: use app 30+ mins, verify dialog. |
| E2E | Optional: parent sets reminder in web, child hits threshold in app, sees dialog. |

---

## Rollout

1. **Feature flag:** `screen_time_reminder.enabled` (backend + mobile) to gate rollout.
2. **Gradual:** Ship to internal testers first; then beta; then GA.
3. **Docs:** Short parent FAQ: "How screen time reminders work" (Android vs iOS behavior).

---

## Future Enhancements

- **Family Controls (iOS):** Apply for Apple entitlement; add true cross-app tracking.
- **Scheduled reminders:** "Remind at 4 PM" independent of usage.
- **Weekly report:** Email parent "This week your child was reminded X times."
