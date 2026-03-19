# Frontend–Backend Flow Integration Report

Investigation of which user flows are fully integrated and work end-to-end across mobile, admin, and web.

---

## 1. Fully Integrated Flows

### Mobile App

| Flow | Backend | Frontend | Status |
|------|---------|----------|--------|
| **Login / Register** | `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/me`, OTP, passwordless | `LoginScreen`, `RegisterScreen`, `AuthViewModel` | Fully integrated |
| **Library stories** | `GET /stories/library` (now paged), `GET /stories/library/{id}` | `LibraryScreen`, `StoryRepository` | Fully integrated |
| **My stories** | `GET /stories` (paginated) | `StoryViewModel`, Dashboard | Fully integrated |
| **Story generation** | `POST /stories/generate` | `StoryGenerationScreen` | Fully integrated; handles 402 upgrade |
| **Audio playback** | `stream-url`, `playback-position`, `narration-script`, `stream-analytics` | `AudioPlayerScreen` | Fully integrated |
| **Favorites** | `GET/POST/DELETE /favorites` | `FavoritesScreen` | Fully integrated |
| **Subscription** | `GET /subscription`, checkout, cancel | `SubscriptionScreen` | Fully integrated |
| **Voice upload** | Voice API | `VoiceUploadScreen`, `MyVoiceAndAvatarScreen` | Fully integrated |
| **Avatar** | Avatar API | `AvatarUploadScreen` | Fully integrated |
| **Settings** | Consent, data export, listening progress | `SettingsScreen` | Fully integrated |
| **Short content** | `/short-content`, `/short-content/daily` | `ShortContentScreen` | Fully integrated |

### Web App

| Flow | Backend | Frontend | Status |
|------|---------|----------|--------|
| **Login (passwordless)** | `POST /auth/passwordless`, `POST /auth/passwordless/verify` | `Login.tsx` – email → code → verify | Fully integrated |
| **Register** | `POST /auth/register` | `Register.tsx` | Fully integrated |
| **Dashboard** | `getRecommendedStories`, `getRecentPlayback` | `Dashboard.tsx` | Fully integrated |
| **Stories** | Library, my stories, favorites, stream, playback, generate, remix | `Stories.tsx` | Fully integrated |
| **Subscription** | Full subscription API | `Subscription.tsx` | Fully integrated |
| **Voice** | Voice profiles, upload | `Voice.tsx` | Fully integrated |
| **Settings** | Consent, data export | `Settings.tsx` | Fully integrated |

### Admin

| Flow | Backend | Frontend | Status |
|------|---------|----------|--------|
| **Story library CRUD** | Full CRUD, bulk publish/delete | `stories/page.tsx`, `[id]/edit` | Fully integrated |
| **Story to speech** | Pipeline, regenerate, retry, approve | `stories/to-speech/page.tsx`, `approve/page.tsx` | Fully integrated |
| **Bulk generate** | Bulk job API | `stories/bulk-generate/page.tsx` | Fully integrated |
| **Short content** | CRUD, generate | `short-content/page.tsx` | Fully integrated |
| **Voice logs** | `getVoiceLogs` | `voice-logs/page.tsx` | Fully integrated |
| **AI metrics** | `getAiMetrics` | `ai-metrics/page.tsx` | Fully integrated |
| **Health** | Actuator/health | Admin health view | Fully integrated |

---

## 2. Partial Integration / Gaps

### Web – Magic link (click-to-sign-in)

| Issue | Detail |
|-------|--------|
| **Broken flow** | `requestMagicLink()` calls `POST /auth/magic-link/request`; `MagicLinkVerify` calls `GET /auth/magic-link/verify?token=`. Neither endpoint exists on the backend. |
| **Actual backend** | Backend has `POST /auth/passwordless` and `POST /auth/passwordless/verify` (code-based). Email includes both a link and a 6-digit code. |
| **Link target** | Backend sends link to `$webBase/login?token=$token`, but the Login page does not read the token; it only shows the code entry form. |
| **Working path** | Code-based flow: user enters email → receives code → enters code on Login page → verified. |

**Conclusion:** The “magic link” click-to-sign-in path is not implemented. Only the code-based path works.

### Admin – Mock API

When `NEXT_PUBLIC_USE_MOCK_API=true`:

| Page | Uses mock | Real API when mock off |
|------|-----------|-------------------------|
| Dashboard | `getMockDashboardKpis`, `getMockRevenueChart`, `getMockStoryUsageChart` | Yes |
| Moderation | `getMockStories`, `getMockStoryBody` | Yes |
| Parents | `getMockParents` | Yes |
| Subscriptions | `getMockSubscriptions`, `getMockInvoices` | Yes |
| Referral codes | Mock | Yes |
| Monitoring | Mock | Yes |
| Revenue | Mock charts/metrics | Yes |

Production should run with `NEXT_PUBLIC_USE_MOCK_API=false` (default), so real APIs are used.

### Mobile – SampleData fallback

| Location | Behavior |
|----------|----------|
| `DashboardScreen`, `TamixaNavHost` | When `storyViewModel.allStories()` is empty, falls back to `SampleData.sampleStories()` and `SampleData.categories`. |
| `AudioPlayerScreen` | Uses `SampleData.sampleStories().first()` in a preview/placeholder. |

**Impact:** When the API returns no stories, the app shows sample data instead of an empty state or messaging. This can be desirable for demos but can hide real empty states.

---

## 3. Summary

| Area | Fully integrated | Gaps |
|------|------------------|------|
| **Mobile** | Auth, library, my stories, story gen, playback, favorites, subscription, voice, avatar, settings, short content | SampleData used when stories empty; preview/placeholder in AudioPlayer |
| **Web** | Login (code), register, dashboard, stories, subscription, voice, settings | Magic-link endpoints and flow not implemented; only code-based login works |
| **Admin** | Story library, to-speech, bulk, short content, voice logs, AI metrics, health | Mock data when `NEXT_PUBLIC_USE_MOCK_API=true`; default uses real API |

---

## 4. Recommendations

1. **Web magic link:** Either:
   - Add backend endpoints `POST /auth/magic-link/request` and `GET /auth/magic-link/verify?token=` and update `MAGIC_LINK_BASE_URL` to `/auth/magic-link`, or
   - Remove or deprecate `requestMagicLink` and `MagicLinkVerify`, and ensure backend emails point users to the code flow only.
2. **Admin:** Ensure production uses `NEXT_PUBLIC_USE_MOCK_API=false`.
3. **Mobile SampleData:** Optionally gate SampleData behind a dev/demo flag, or provide an explicit empty state when the API returns no stories.
