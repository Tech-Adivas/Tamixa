# Tamixa — End-to-End Test Report (26 Sep 2026)

**Scope:** backend API, PostgreSQL + Redis, parent web, admin dashboard, story-pipeline, mobile (static review).
**Code tested:** the working tree on the MacBook on 26 Sep 2026 — last commit `c30c8d5` **plus uncommitted changes** (~30 modified and several untracked files, mainly in `admin/` and `backend/`).
**Environment:** Linux sandbox, JDK 21, Node 22, Postgres 16, Redis 7, Chromium (Playwright). Real `.env` values were used; outbound calls to AI providers were blocked by the sandbox network, so AI/TTS/S3 were **not** exercised.

## 0. Fix status (updated 26 Sep 2026, same day)

All items below were fixed in the working tree and re-tested in the sandbox. Originals of every touched file are saved in `_fixes-2026-09-26/originals-backup.tgz` (list in `manifest.txt`).

| ID | Fix | Files | Re-test result |
|---|---|---|---|
| A1 | Added shadcn-style `Textarea` component | `admin/src/components/ui/textarea.tsx` (new) | Review page renders; `next build` passes |
| A2 | Fixed 3 TS errors + 1 React 19 `useRef` typing | `stories/audio/page.tsx`, `story-status-badge.tsx` (title moved to wrapper span), `stories/new/page.tsx` | `tsc --noEmit` clean |
| A3 | Removed duplicate `src/app/favicon.ico` (identical to `public/`) | `admin/src/app/favicon.ico` (deleted), `layout.tsx` comment | `/favicon.ico` → 200 |
| A4 (new) | Content Review "Approve" called the **AI-story** endpoint `/admin/stories/{id}/approve`; now calls library workflow `/library/stories/{id}/approve` | `admin/src/lib/api.ts` | Returns proper 400 when languages not reviewed; no longer touches AI stories |
| S1 | Admin: Next 14.2.35 → **15.5.26**, React 18 → **19**, eslint-config-next 15.5.26. Web: vitest → 4.1.11, Vite 5 → **7**, plugin-react 5, lockfile regenerated | `admin/package*.json`, `web/package*.json`, `admin/tsconfig.json` (target ES2017) | `npm audit --audit-level=critical` passes in both; web 2 moderate, admin 1 high + 1 moderate left (need Next 16 / react-router 7) |
| B1 | Added handlers: AccessDenied → 403, missing param/header, type mismatch, bad JSON → 400, unknown URL → 404, 415, plus domain exceptions (StoryAccessDenied, FamilyVoice*, AvatarAccessDenied, SubscriptionNotFound, NarrationJobCapacity → 503) | `GlobalExceptionHandler.kt` | 87-endpoint sweep ×2 roles: **0 × 500** (was 41) |
| B2 | Admin `status` filter parses unified statuses + comma lists; legacy PROCESSING→TRANSLATING, READY→APPROVED; unknown → 400. "Processing first" sort and review-queue query moved to unified statuses. Admin pages now request `CONTENT_REVIEW` (review) and `APPROVED,AUDIO_*` (audio); Story library filter options updated | `StoryLibraryService.kt`, `LibraryStoryJpaRepository.kt`, admin `stories/page.tsx`, `review/page.tsx`, `audio/page.tsx` | all status filters 200; `BOGUS` → 400 |
| B3 | FREE subscription created in its own `REQUIRES_NEW` tx; loser of the race re-reads. Found and fixed a second race in `usage_tracking` (`INSERT … ON CONFLICT DO NOTHING`) | `FreeSubscriptionCreator.kt` (new), `SubscriptionService.kt`, `UsageTracking*` | 3 rounds × 20 concurrent first calls: 60/60 × 200, exactly 1 row each |
| B4 | `app.api-exposure.swagger-public` (true only in dev profile; forced false in staging/prod) | `SecurityConfig.kt`, `AppProperties.kt`, `application*.yml` | dev: Swagger + api-docs open |
| B5 | JVM zone `Asia/Calcutta` (and other legacy aliases) normalised to `Asia/Kolkata` before any DB connection | `TamixaApplication.kt` | starts cleanly on a JVM reporting Asia/Calcutta |
| W1/W2 | `let`→`const`; Dashboard test text updated | 2 web test files | lint clean; 49/49 tests |
| E1 | Admin smoke spec rewritten (+ optional login test via `ADMIN_EMAIL`/`ADMIN_PASSWORD`) | `e2e/admin/smoke.spec.ts` | Playwright 7/7 pass |
| M1 | Release signing from `TAMIXA_UPLOAD_*` in `mobile/local.properties` or env; unsigned when unset | `mobile/composeApp/build.gradle.kts` | Gradle config OK; `signingReport` shows keystore for `prodRelease` |
| M3 | Correct Gradle task names in docs | `docs/GETTING_STARTED.md`, `docs/DEPLOYMENT_PLAN.md` | — |
| C1 | Kafka/Zookeeper behind compose profile `kafka`; backend no longer waits for Kafka | `docker-compose.yml` | `docker compose config` OK |
| C2 | First duplicate `SENDGRID_API_KEY` in `.env` commented out (the later one was already the effective value) | `.env` (local only) | behaviour unchanged |
| C3 | 20 root notes → `docs/archive/`, 6 ad-hoc SQL → `scripts/sql/`, references updated | root, `docs/ARCHITECTURE_AND_FLOWS.md`, `docs/COMPLIANCE.md` | root now holds only `AGENTS.md` |

**Still open (needs you):**
- **M2** prod domain mismatch (`api.tamixa.in` vs `app.tamixa.com`) — tell me the real domains and I'll align the defaults.
- **Backend unit tests** — run `./gradlew :backend:test -Ptamixa.backendOnly=true` on your Mac (test libraries were not available offline in the sandbox).
- **Android build/tests** — run on your Mac (commands in §3).
- **Real AI/TTS/S3 keys** — `GET /api/v1/dev/verify-connections`.
- **Pipeline end-to-end** (submit → translate → approve → audio → publish) needs real AI keys; the approve step was verified to enforce "all languages reviewed".
- **D1** old dump needs Postgres 18 client (documented).
- After pulling these changes run `npm install` in `admin/` and `web/` (dependency upgrades).

## 1. Summary

| Area | Result | Notes |
|---|---|---|
| Flyway migrations (fresh DB) | ✅ PASS | 100/100 applied, 87 tables, 70 seed stories (all DRAFT) |
| Duplicate-migration guard | ✅ PASS | |
| Backend compile + boot jar | ✅ PASS | Starts in ~35 s; health UP |
| Backend with minimal env (no keys) | ✅ PASS | Starts and is healthy |
| Backend unit tests | ⚠️ NOT RUN | Test dependencies not in local Gradle cache and Maven Central blocked in sandbox. Run locally: `./gradlew :backend:test -Ptamixa.backendOnly=true` |
| Auth flows (admin login, register, login, refresh, OTP, email code, logout + revocation) | ✅ PASS | Logout revocation stored in Redis works |
| Full GET-endpoint sweep (87 endpoints, admin + parent tokens) | ❌ ISSUES | ~40 wrong 500s — see B1, B2, B3 |
| Web: lint | ❌ FAIL | 1 error (W1) |
| Web: unit tests | ❌ FAIL | 48/49 pass (W2) |
| Web: build | ✅ PASS | |
| Web: browser run (login → 8 pages) | ✅ PASS (1 API error) | B3 on Subscription page |
| Admin: lint | ✅ PASS | |
| Admin: type check / production build | ❌ FAIL | A1, A2 |
| Admin: unit tests | ✅ PASS | 29/29 |
| Admin: browser run (login → 25 pages) | ⚠️ PASS after workaround | A1 breaks all pages in dev until fixed; B2 on Review/Audio pages |
| E2E Playwright smoke (repo) | ⚠️ 3/5 | 2 admin tests are outdated (E1) |
| story-pipeline typecheck + build | ✅ PASS | |
| npm audit (CI gate `--audit-level=critical`) | ❌ FAIL | web + admin each have 1 critical (S1) |
| Mobile Android build/tests | ⚠️ NOT RUN | Needs Android SDK (macOS aapt2 only in cache). Static review done (M1–M3) |
| DB dump restore (`tamixa.local.dump`) | ❌ FAIL | Made with pg_dump 18; needs Postgres 18 client (D1) |
| AI / TTS / S3 / SendGrid key check | ⚠️ NOT RUN | Run `GET /api/v1/dev/verify-connections` on your Mac |

## 2. Bugs found (priority order)

### Blockers for next deploy

**A1 — Admin build breaks: missing `components/ui/textarea`** (untracked file `admin/src/app/(dashboard)/dashboard/stories/review/page.tsx:11`).
`next build` fails with *Module not found*. In `npm run dev`, visiting Review shows a Build Error overlay that then blocks every admin page. Fix: add `admin/src/components/ui/textarea.tsx` (shadcn Textarea) or change the import.

**A2 — Admin TypeScript errors fail `next build`:**
- `stories/audio/page.tsx:38,43` — required parameter after optional parameter (TS1016)
- `stories/review/page.tsx:716` — implicit `any` for `e`
- `components/design-system/story-status-badge.tsx:179` — passes `title` to an icon component that only accepts `className`

**S1 — CI `npm audit --audit-level=critical` fails:** web → `vitest` (non-breaking fix available: `npm audit fix`); admin → `next@14.2.35` (fix requires major upgrade; decide upgrade path or adjust gate). Plus 8 high (web) and 12 high (admin).

### Backend

**B1 — Wrong HTTP status codes (500 instead of 4xx).** `GlobalExceptionHandler` has no handlers for:
- `AccessDeniedException` → 500 "Access Denied" (should be **403**). Seen on 30+ parent endpoints when called with an admin token.
- `MissingServletRequestParameterException` / type mismatch → 500 (should be **400**). e.g. `/stories/search` without `q`.
- `NoResourceFoundException` → 500 (should be **404**) for unknown URLs.
Effect: noisy error logs/alerts, clients cannot tell bugs from bad input.

**B2 — Admin story lists crash on legacy status names (after V99 unified status).**
- `GET /api/v1/admin/stories?status=READY` → 500 (`READY` not in `LibraryStoryStatus`). Used by admin **Content Review** page.
- `GET /api/v1/admin/stories?status=PUBLISHED` → 500 "No enum constant … PROCESSING". `StoryLibraryService.findAllByStatus` (line ~1796) queries `listOf("PUBLISHED","PROCESSING","READY")`. Used by admin **Audio Generation** page.
Fix: map legacy names via `LibraryStoryStatus.fromString` and remove `PROCESSING`/`READY` from the query list; update admin pages to new statuses.

**B3 — Race condition creating free subscription.** Web calls `/subscription` and `/subscription/usage` at the same time for a new user; both run `SubscriptionService.getOrCreateSubscription` → second insert hits `uq_subscriptions_parent_id` → 500. Fix: catch `DataIntegrityViolationException` and re-read, or `INSERT … ON CONFLICT DO NOTHING`.

**B4 — Swagger always needs login**, but `docs/GETTING_STARTED.md` says it is open at `/swagger-ui.html`. Either allow it in the dev profile or update docs.

**B5 — JVM timezone `Asia/Calcutta` rejected by Postgres** (`FATAL: invalid value for parameter "TimeZone"`). Machines reporting the old IANA name cannot start the backend. The Postgres JDBC driver sends the JVM default timezone when connecting. Fix: `-Duser.timezone=Asia/Kolkata` (or UTC) in run config / Dockerfile, or `TimeZone.setDefault(...)` at the top of `main()`.

### Web

**W1 — Lint:** `web/src/lib/financialRealityEngine.test.ts:36` — `let e` should be `const` (CI lint step fails).
**W2 — Test:** `web/src/pages/Dashboard.test.tsx:55` expects "Jump back into the tale you paused", page now says "Jump back in, discover tonight's picks…". Update test.

### Admin (minor)

**A3 — `/favicon.ico` returns 500** in dev: both `admin/src/app/favicon.ico` and `admin/public/favicon.ico` exist (Next.js conflict). Delete one.

### E2E

**E1 — `e2e/admin/smoke.spec.ts` outdated:** admin root page now shows an "Admin login" button (no email field), and unauthenticated redirect goes to `/login?expired=1`. Update both tests.

### Database

**D1 — `tamixa.local.dump` needs pg_restore 18** (`unsupported version (1.16)`); compose/CI use Postgres 16. Document it or re-dump with a 16 client.

### Mobile (static review)

**M1 — No release `signingConfig`** in `mobile/composeApp/build.gradle.kts` → Play Store bundle can't be signed from Gradle.
**M2 — Domain mismatch in prod defaults:** API `https://api.tamixa.in` vs web `https://app.tamixa.com`; QA uses `tamixa.com`. Confirm the intended domains.
**M3 — Doc drift:** `docs/GETTING_STARTED.md` says `./gradlew :mobile:androidApp:assembleDebug`, but `settings.gradle.kts` includes only `:mobile:composeApp` (the `mobile/androidApp/` folder is not part of any build). Correct task: `./gradlew :mobile:composeApp:assembleDevDebug` (or `cd mobile && ./gradlew :composeApp:assembleDevDebug`). `docs/DEPLOYMENT_PLAN.md` uses `:composeApp:assembleRelease` from root — should be `bundleProdRelease` from `mobile/`.

### Config / hygiene

**C1 — docker-compose backend waits for Kafka (+ Zookeeper) to be healthy**, but the backend has no Kafka client (publishers are inline/no-op). Adds ~1 GB RAM and startup time for nothing. Make Kafka a compose profile.
**C2 — Duplicate `SENDGRID_API_KEY` lines** in the local `.env` (lines 50 and 128) with different values — the second silently wins.
**C3 — Root clutter:** 20+ one-off `*_SUMMARY.md`/`*_FIX.md` files and ad-hoc `.sql` scripts at repo root, 1 MB of `backend*.log`. Move to `docs/archive/` and `scripts/sql/`.
**C4 — Auth rate limit** triggers after ~10 auth calls from one IP in a burst (429). Fine for prod; worth documenting for QA automation.

## 3. What to run on your Mac to close the gaps

```bash
# 1. backend tests
./gradlew :backend:test -Ptamixa.backendOnly=true
# 2. mobile compile + unit tests
./gradlew :mobile:composeApp:compileDevDebugKotlinAndroid
cd mobile && ./gradlew :composeApp:testDevDebugUnitTest :composeApp:assembleDevDebug
# 3. real provider keys (backend running, dev profile)
curl http://localhost:8080/api/v1/dev/verify-connections | jq
# 4. publish one story end-to-end in admin, then play it on web + emulator (guide §9.2)
```

## 4. Evidence

- Backend startup: `Successfully applied 100 migrations … now at version v100`; `Started TamixaApplicationKt in 36.3 seconds`.
- Endpoint sweep: 87 GET endpoints × 2 roles; 40 × 200, 41 × 500 (all traced to B1/B2), 3 × 503 (sandbox network), 2 × 400, 1 × 404.
- Admin browser run: 25 dashboard pages rendered with seeded admin after A1 workaround; web browser run: login with email code + consent → Home, Stories, Voice, Avatar, Subscription, Settings, Privacy, Terms rendered.
