# Google Play Store 3-Day Release Action Plan

**Target:** Release Tamixa Kids on **Sunday**  
**Timeline:** Thursday → Friday → Saturday (3 days)  
**Focus:** Mobile app (`com.tamixa.android`) first, backend & compliance aligned

---

# Executive Summary

| Area | Status | Critical Gaps |
|------|--------|---------------|
| **Store assets** | ❌ Not ready | Custom icon, splash, feature graphic, screenshots |
| **Release signing** | ❌ Missing | Keystore config; currently builds unsigned APK/AAB |
| **Privacy & compliance** | ❌ Missing | Privacy policy URL; COPPA/Family policy declaration |
| **Backend** | ✅ Ready | Prod env vars, CORS, JWT validation |
| **App functionality** | ✅ Core ready | Auth, stories, voice, subscription, settings |
| **Testing** | ⚠️ Partial | Backend integration; no mobile tests; no E2E |

---

# Day 1 — Thursday: Store Blockers & Compliance

## 1.1 Release Signing (CRITICAL)

**Current state:** Build produces `composeApp-release-unsigned.apk`. Play Store requires signed app bundles.

| Task | Action |
|------|--------|
| Create keystore | `keytool -genkey -v -keystore tamixa-release.keystore -alias tamixa -keyalg RSA -keysize 2048 -validity 10000` |
| Add `key.properties` | Create `mobile/key.properties` (exclude from git) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword` |
| Update `build.gradle.kts` | Add `signingConfigs.release` and `buildTypes.release.signingConfig` |
| Build AAB | `./gradlew :composeApp:bundleRelease` → `build/outputs/bundle/release/composeApp-release.aab` |

**Note:** Play Store prefers **AAB (Android App Bundle)** over APK for optimized delivery.

---

## 1.2 App Icon & Splash (CRITICAL)

**Current state:** `@android:drawable/ic_btn_speak_now` (system icon) — Play Store will reject or looks unprofessional.

| Asset | Specs | Action |
|-------|-------|--------|
| App icon | 48dp base; provide mdpi(48), hdpi(72), xhdpi(96), xxhdpi(144), xxxhdpi(192) | Replace in `mobile/composeApp/src/androidMain/res/` (mipmap-*) |
| Adaptive icon | `mipmap-anydpi-v26` for foreground + background | Required for Android 8+ |
| Splash screen | CoreSplashScreen API or drawable | Add `ic_launcher_foreground` + splash theme |

**Quick path:** Use Android Studio’s Image Asset Studio or a generator (e.g., appicon.co) for all densities.

---

## 1.3 Privacy Policy (REQUIRED)

**Play Store requirement:** Must have a privacy policy URL on the store listing **and** in the app (Settings or About).

| Task | Action |
|------|--------|
| Draft policy | Include: data collected (email, child profiles, voice samples, story data), how used, no selling to third parties, COPPA compliance, retention |
| Host policy | Publish at `https://app.tamixa.com/privacy` or `https://tamixa.com/privacy` |
| In-app link | Add “Privacy Policy” link in Settings screen |
| Store listing | Add URL in Play Console → Store presence → App content |

**COPPA / Kids app:** Declare target age group; no behavioral ads; no sharing PII with third parties for ads.

---

## 1.4 Security Hardening

| Task | Action |
|------|--------|
| Disable cleartext | In `AndroidManifest.xml`, set `android:usesCleartextTraffic="false"` for release (or use build flavor) |
| API URL | Ensure release build uses `https://api.tamixa.com` via `TAMIXA_API_BASE_URL` |
| ProGuard | Verify release build works; mapping.txt saved for crash symbolication |

---

# Day 2 — Friday: Polish, Backend Prod & Testing

## 2.1 Missing Functionalities (Assess & Fix)

| Feature | Status | Action |
|---------|--------|--------|
| Offline / cached stories | Check if cached stories play without network | Add basic offline handling or clear “Need internet” message |
| Error states | Network / API errors | Ensure user-facing messages (no raw stack traces) |
| Subscription web redirect | Opens `SUBSCRIPTION_WEB_URL` | Verify URL correct for prod (`https://app.tamixa.com/subscription`) |
| Deep links | None | Optional; not blocking for v1 |
| Push notifications | Firebase not configured | Optional; can add post-launch |

---

## 2.2 UI Improvements

| Area | Action |
|------|--------|
| Loading states | All async screens (story gen, voice upload) have spinners/placeholders |
| Empty states | Dashboard, story list, child list show friendly empty messages |
| Accessibility | Add content descriptions for key buttons (accessibility tree) |
| Kid-safe UI | Large touch targets, no accidental exits |
| Consistent theming | TamixaColors, TamixaEmojiDisplay used across all screens |

---

## 2.3 Backend Production Verification

| Task | Command / Check |
|------|-----------------|
| Health endpoints | `curl https://api.tamixa.com/actuator/health` → UP |
| Env vars set | `JWT_SECRET` (non-default), `CORS_ALLOWED_ORIGINS`, `DATABASE_*`, `OPENAI_API_KEY` |
| CORS | Include `https://app.tamixa.com` and mobile origin if needed |
| Rate limits | Story generation limits in place (free/paid tiers) |
| Flyway | All migrations applied; `ddl-auto: validate` |

---

## 2.4 Testing

| Type | Action |
|------|--------|
| Manual smoke | Register → Login → Create child → Generate story → Play audio → Voice upload → Subscription → Settings → Logout |
| Backend tests | `./gradlew :backend:test -Ptamixa.backendOnly=true` (Docker required) |
| Device testing | Test on real device (min 2 devices; different Android versions) |
| Subscription flow | Verify subscription page loads; payment links work |

---

# Day 3 — Saturday: Store Listing & Submission

## 3.1 Store Assets

| Asset | Specs | Notes |
|-------|-------|-------|
| Feature graphic | 1024 x 500 px | One required |
| Screenshots | Min 2; max 8; phone + tablet if supported | 320–3840 px |
| Short description | Max 80 chars | Store listing |
| Full description | Max 4000 chars | Store listing |
| App icon | 512 x 512 px | Play Console |

---

## 3.2 Play Console Setup

| Section | Action |
|---------|--------|
| App signing | Enroll in Play App Signing (recommended) |
| Content rating | Complete questionnaire (Kids / Family category) |
| Target audience | Declare age groups (e.g., “Ages 5 and under”, “Ages 6–12”) |
| Ads declaration | Declare “No ads” or “Contains ads” if applicable |
| Data safety | Declare data collected (email, child names, voice samples, etc.) |
| Privacy policy | Add URL |
| Store listing | Title, short desc, full desc, screenshots, feature graphic |

---

## 3.3 Final Build & Upload

```
cd mobile
./gradlew :composeApp:bundleRelease \
  -PTAMIXA_API_BASE_URL=https://api.tamixa.com \
  -PTAMIXA_WEB_APP_URL=https://app.tamixa.com
```

Upload `composeApp-release.aab` to Play Console → Production (or Internal testing first).

---

# Missing Functionalities (Summary)

| Priority | Item | Notes |
|----------|------|-------|
| P0 | Release signing | Blocking |
| P0 | App icon | Blocking |
| P0 | Privacy policy | Blocking |
| P0 | Content rating | Blocking |
| P1 | Splash screen | Professional polish |
| P1 | Feature graphic + screenshots | Store visibility |
| P1 | `usesCleartextTraffic=false` | Security |
| ~~P2~~ | ~~Crash reporting (Firebase Crashlytics)~~ | ✅ Implemented |
| ~~P2~~ | ~~Offline behavior for cached stories~~ | ✅ Implemented (persistent cache + ExoPlayer disk cache) |
| P2 | E2E tests | Quality assurance |

---

# Performance Checklist

| Item | Action |
|------|--------|
| App size | AAB + R8 + resource shrinking → keep under ~50 MB |
| Cold start | Measure; ensure < 3 s on mid-range device |
| Memory | Profile story list / audio playback; no leaks |
| Network | Retry logic for failed API calls |

---

# Security & Compliance

| Area | Status | Action |
|------|--------|--------|
| JWT storage | EncryptedSharedPreferences | ✅ Done |
| Password hashing | BCrypt | ✅ Done |
| CORS | Prod validation | ✅ Done |
| Rate limiting | Backend | ✅ Done |
| COPPA | Kids app | Draft privacy policy; declare no sale of child data |
| Data safety form | Play Console | Fill accurately |

---

# End-to-End Release Checklist

## Pre-submission
- [ ] Keystore created; `key.properties` configured
- [ ] Signed AAB builds successfully
- [ ] Custom app icon in place
- [ ] Splash screen (optional but recommended)
- [ ] Privacy policy hosted and linked in app + store
- [ ] `usesCleartextTraffic` disabled for release
- [ ] Release build points to `https://api.tamixa.com`

## Store listing
- [ ] App name, short description (80 chars), full description (4000 chars)
- [ ] Feature graphic 1024x500
- [ ] Screenshots (min 2 for phone)
- [ ] Content rating questionnaire completed
- [ ] Target audience / age groups declared
- [ ] Ads declaration (Yes/No)
- [ ] Data safety form completed
- [ ] Privacy policy URL added

## Backend
- [ ] Production API live and healthy
- [ ] `JWT_SECRET` changed from default
- [ ] `CORS_ALLOWED_ORIGINS` set for `app.tamixa.com`
- [ ] Database migrations applied

## Testing
- [ ] Full smoke test on real device (Register → Story playback → Logout)
- [ ] Subscription flow works (opens web correctly)
- [ ] Voice upload works
- [ ] Settings (dark mode, logout) persist

## Submission
- [ ] AAB uploaded to Play Console
- [ ] Production track selected (or Internal testing first)
- [ ] All store listing sections complete
- [ ] Submit for review

---

# Deliverable Notes

| Deliverable | Location / Command |
|-------------|-------------------|
| Signed AAB | `mobile/composeApp/build/outputs/bundle/release/composeApp-release.aab` |
| ProGuard mapping | `mobile/composeApp/build/outputs/mapping/release/mapping.txt` (save for Crashlytics/symbolication) |
| Privacy policy | Host at `https://app.tamixa.com/privacy` or equivalent |
| Build script | `./scripts/build-production.sh --mobile-only` |

---

# Quick Commands Reference

```bash
# Build signed release AAB
cd mobile
./gradlew :composeApp:bundleRelease \
  -PTAMIXA_API_BASE_URL=https://api.tamixa.com \
  -PTAMIXA_WEB_APP_URL=https://app.tamixa.com

# Backend tests
./gradlew :backend:test -Ptamixa.backendOnly=true

# Full production build
./scripts/build-production.sh
```

---

---

# Recent Improvements (Post-plan)

- **Offline behavior:** `StoryCacheDataStore` persists stories to SharedPreferences; ExoPlayer uses disk cache (100 MB) for streamed audio—previously played stories play offline.
- **Crashlytics:** Firebase Crashlytics integrated. Replace `google-services.json` placeholder with your project config from Firebase Console.
- **Zoho Payments:** Razorpay replaced with Zoho Payments. Webhook: `POST /api/v1/webhooks/zoho`. Env: `ZOHO_ENABLED`, `ZOHO_ACCOUNT_ID`, `ZOHO_WEBHOOK_SIGNING_KEY`. Pass `parent_id` in payment meta_data when creating sessions/links.

*Document created for Tamixa Kids Google Play Store release. Update as tasks complete.*
