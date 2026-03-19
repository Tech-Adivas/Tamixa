# Mobile Application Audit Report

**Project:** Tamixa (Compose Multiplatform — Android / iOS)  
**Audit date:** March 2025  
**Scope:** Security, architecture, code quality, UX/accessibility, performance, testing, compliance

---

## Executive summary

The Tamixa mobile app is a Kotlin Multiplatform (KMP) application with a clear separation of network, repository, and UI layers. Security practices are largely aligned with project rules (encrypted token storage on Android, centralized logging with PII guidance, no hardcoded secrets). **Critical findings** include iOS token storage in plain NSUserDefaults and **no automated tests** in the mobile module. **Recommendations** focus on encrypting iOS tokens, adding unit/integration tests for auth and payments, and tightening a few logging and error-handling paths.

---

## 1. Security

### 1.1 Authentication & token storage

| Area | Status | Details |
|------|--------|--------|
| Android token storage | ✅ Good | `EncryptedSharedPreferences` with AES256_GCM (MasterKey). Keys: `access_token`, `refresh_token`, `token_expiry_ms`. |
| iOS token storage | ⚠️ **High** | **Tokens stored in `NSUserDefaults` (plaintext).** Keychain is the standard for sensitive data on iOS. Recommend migrating to Keychain (e.g. `Security` framework or a KMP-friendly wrapper). |
| Token refresh | ✅ Good | Ktor Auth bearer plugin with `refreshTokens`; refresh client closed in `finally`. Refresh uses dedicated client (no token in default request until refreshed). |
| Logout / clear | ✅ Good | `TokenStorage.clear()` used on logout; no tokens left in storage. |

### 1.2 Secrets & configuration

| Area | Status | Details |
|------|--------|--------|
| API base URL | ✅ Good | From `BuildConfig` (Android) / platform config; debug from `local.properties` (TAMIXA_API_BASE_URL), release from project property. No hardcoded production URLs in code. |
| No API keys in repo | ✅ Good | No API keys or passwords in source; backend URL and subscription web URL are build-time config. |
| ProGuard (release) | ✅ Good | Minify + shrink enabled; keep rules for `com.tamixa.domain.**`, `com.tamixa.network.**`, Kotlin serialization. |

### 1.3 Logging & PII

| Area | Status | Details |
|------|--------|--------|
| TamixaLog policy | ✅ Good | Centralized logging; doc explicitly forbids PII, passwords, OTP, tokens; `maskUrl`, `maskLast4` for identifiers. |
| Auth flows | ✅ Good | AuthViewModel and AuthRepository do not log email, phone, or codes; only "login failed" / "requestPasswordlessCode failed" etc. |
| Ktor client logging | ⚠️ Medium | `LogLevel.INFO` may log request URLs and possibly headers. Ensure Ktor’s logger does **not** log `Authorization` (or use a custom logger that redacts it). Prefer DEBUG for body/headers in dev only. |
| TtsSynthesizer | ⚠️ Low | Logs `textLen=${text.length}` — if story text were ever logged, that could be content/PII; currently only length is logged. |
| Android `Log` usage | ✅ Acceptable | FamilyVoiceRecordDialog uses `Log.d`/`Log.e` for MediaRecorder lifecycle/errors only; no PII. |

### 1.4 Input & API usage

| Area | Status | Details |
|------|--------|--------|
| Phone input | ✅ Good | Digit-only, length capped to `PHONE_NUMBER_MIN_LENGTH` (10). |
| Email/code validation | ⚠️ Medium | No client-side format validation for email or OTP code length before API call. Recommend basic email format and code length checks to fail fast and reduce unnecessary requests. |
| Backend errors to user | ✅ Good | `errorMessageForUser()` maps 401/403/404/429/5xx to generic strings; avoids leaking stack traces or internal details. |
| Raw exception message | ⚠️ Low | For non-ResponseException, `throwable.message` is shown. Backend should avoid sensitive data in error messages; app could fallback to `Strings.somethingWentWrong()` for unknown errors. |

### 1.5 Network

| Area | Status | Details |
|------|--------|--------|
| HTTPS in production | ✅ Good | Release `BASE_URL` from `TAMIXA_API_BASE_URL` (e.g. https://api.tamixa.com); no HTTP in production config. |
| Certificate pinning | ❌ Not present | No pinning implemented. Consider pinning for high-assurance builds (e.g. subscription/payments). |
| Timeouts & retry | ✅ Good | Connect/socket timeout 30s; retry on server errors with exponential delay; `ApiConfig` constants. |

---

## 2. Architecture & code quality

### 2.1 Structure

| Area | Status | Details |
|------|--------|--------|
| KMP layout | ✅ Good | `commonMain` (network, repo, UI, viewmodels), `androidMain` / `iosMain` for platform (token storage, TTS, playback, permissions). |
| DI | ✅ Good | Koin; shared + platform + viewModel modules; `appScope` for coroutines. |
| Layer separation | ✅ Good | API layer (Ktor), repository (Auth, Story, etc.), ViewModels; no business logic in composables. |

### 2.2 Error handling

| Area | Status | Details |
|------|--------|--------|
| Result / runCatching | ✅ Good | Repositories use `Result` / `runCatching`; ViewModels use `fold(onSuccess, onFailure)` and set `UiState.Error` with message. |
| Silent catches | ✅ None found | No empty `catch {}` blocks; failures are logged and/or surfaced. |
| Crash risk | ⚠️ Low | Unchecked casts in AvatarUploadScreen and VoiceUploadScreen (`UiState.Success<String>`, etc.); safe if state is always updated from same VM. One `initError!!` on iOS (IosApp.kt) — ensure init path never leaves it null. |

### 2.3 Dependencies

| Area | Status | Details |
|------|--------|--------|
| Versions | ✅ Good | Kotlin 2.x, Compose Multiplatform, Ktor 2.x, Coil 3, Media3; Coil-GIF pinned (3.0.3) for ABI compatibility. |
| Security-related | ✅ Good | Android: `androidx.security.crypto` (EncryptedSharedPreferences), DataStore. |

---

## 3. UX & accessibility

| Area | Status | Details |
|------|--------|--------|
| contentDescription | ⚠️ Partial | Some images/components have descriptions (e.g. onboarding, hero, buttons); not all interactive elements are audited. Recommend audit of all `Icon`, `Image`, and custom clickables. |
| Semantics | ✅ Good | Progress header has `contentDescription = "Step $step of $totalSteps"`; TamixaPrimaryButton uses `semantics { role = Role.Button }`. |
| Touch targets | ✅ Good | Buttons use 56.dp height (≥ 48dp). |
| Screen reader / TalkBack | ⚠️ Not fully verified | Logic is in place for key flows; full a11y pass (TalkBack/VoiceOver) recommended. |

---

## 4. Performance & reliability

| Area | Status | Details |
|------|--------|--------|
| Image loading | ✅ Good | Coil with Ktor fetcher; crossfade. |
| Memory | ✅ Good | Story cache limits (e.g. `CACHE_MAX_STORIES = 50`); cache implementations in commonMain (in-memory) and platform (DataStore/NSUserDefaults). |
| HttpClient | ✅ Good | Single shared client with auth; refresh client created per refresh and closed. |
| Background playback | ✅ Good | Foreground service and MediaPlayback permission declared; ExoPlayer/Media3 on Android. |

---

## 5. Testing

| Area | Status | Details |
|------|--------|--------|
| Unit tests | ❌ **Critical** | **No tests found** under `mobile/**/test/**`. `commonTest` exists but has no tests. |
| Recommendation | — | Add unit tests for: AuthViewModel (login/register/passwordless/OTP state transitions), AuthRepository (token save/clear), ApiConfig (resolveAudioUrl/resolveCoverUrl). Consider integration tests for token refresh and critical API flows. |

---

## 6. Compliance & project rules

| Rule | Status | Details |
|------|--------|--------|
| No secrets in logs | ✅ | TamixaLog policy and masking helpers; no tokens or PII in log calls reviewed. |
| Env for secrets | ✅ | API URL from build/config; no secrets in source. |
| Input validation | ⚠️ | Phone validated; email/code could be strengthened client-side. |
| Error handling | ✅ | No silent catches; errors logged and/or surfaced. |
| Naming conventions | ✅ | Aligned with AGENTS.md / naming-conventions (ViewModels, Screens, *Api, *Repository). |

---

## 7. Recommendations (prioritized)

### High priority

1. **iOS token storage**  
   Store access/refresh tokens and expiry in Keychain instead of NSUserDefaults. Use a small expect/actual or iOS-only Keychain wrapper and keep the same `TokenStorage` interface.  
   **Status:** TODO documented in `IosTokenStorage` and this audit; requires Security framework cinterop or Swift-injected delegate.

2. **Automated tests**  
   Add at least: (a) unit tests for auth flows and token handling, (b) unit tests for URL resolution (ApiConfig), (c) one or two integration tests for token refresh or login success path.  
   **Status:** ApiConfig unit tests added (`commonTest/.../ApiConfigTest.kt`). Auth/token tests can be added next with mocked deps.

### Medium priority

3. **Ktor logging**  
   Confirm that at INFO level the Ktor client does not log the `Authorization` header; if it does, switch to a custom logger that redacts it or lower level for production.

4. **Client-side validation**  
   Validate email format and OTP code length (and possibly format) before calling auth APIs; show clear validation errors.  
   **Status:** Done. `AuthValidation` (email + OTP code), `Strings.invalidEmail()` / `invalidCode()`; AuthViewModel validates before `requestPasswordlessCode`, `verifyPasswordlessCode`, and `loginWithOtp`.

5. **Error messages**  
   For unknown exceptions in `errorMessageForUser`, consider always returning `Strings.somethingWentWrong()` instead of `throwable.message` to avoid leaking server messages.  
   **Status:** Done. Unknown exceptions now always return `Strings.somethingWentWrong()`.

### Lower priority

6. **Certificate pinning**  
   Evaluate pinning for production API (and optionally subscription web) in high-security or regulated scenarios.

7. **Accessibility**  
   Run a full TalkBack/VoiceOver pass and ensure every interactive element and meaningful image has a content description or semantic.

8. **Unchecked casts**  
   Replace unchecked `UiState.Success<T>` casts with a safe accessor (e.g. `dataOrNull()` or sealed when) where possible to avoid accidental ClassCastException.

---

## 8. Summary table

| Category       | Critical | High | Medium | Low |
|----------------|----------|------|--------|-----|
| Security       | 0        | 1 (iOS tokens) | 2 (Ktor log, validation) | 2 |
| Architecture   | 0        | 0   | 0      | 1 (casts) |
| Testing        | 1 (no tests) | 0 | 0 | 0 |
| UX/A11y        | 0        | 0   | 1 (a11y pass) | 0 |

**Conclusion:** The app is in good shape for security (except iOS token storage), structure, and logging discipline. The two main gaps are **iOS token encryption** and **lack of automated tests**; addressing those should be the next focus.
