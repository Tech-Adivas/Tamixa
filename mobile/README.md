# Tamixa – Kotlin Multiplatform Mobile App

Production-ready KMP app for Tamixa (parent auth, child profiles, story generation, audio playback, voice upload, subscription, settings).

## Tech Stack

- **Kotlin Multiplatform** (shared + Android; iOS-ready via Compose Multiplatform)
- **Jetpack Compose** (Android) / **Compose Multiplatform** (shared UI)
- **MVVM** + **Clean Architecture**
- **Ktor** client (auth, refresh, retry, JSON)
- **Secure token storage**: EncryptedSharedPreferences (Android), Keychain-ready (iOS)
- **ExoPlayer** (Android) for audio streaming & background playback
- **Coroutines** + **Flow**
- **Koin** for DI

## Folder Structure

```
mobile/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml          # Version catalog
├── README.md
├── composeApp/                     # Single KMP module (shared + Android app)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/tamixa/
│       │   ├── di/                  # Koin modules (shared, viewModel)
│       │   ├── domain/               # Auth, Child, Story, Voice, Subscription models
│       │   ├── network/              # ApiConfig, HttpClientFactory, AuthApi, ChildApi, StoryApi, VoiceApi
│       │   ├── platform/             # currentTimeMillis expect/actual
│       │   ├── repository/           # Auth, Child, Story (with cache), Voice repos
│       │   ├── security/             # TokenStorage interface (no impl in common)
│       │   ├── ui/
│       │   │   ├── navigation/       # Screen sealed class
│       │   │   ├── screen/           # Login, Register, ChildList, ChildCreate, Story*, Audio, Voice, Subscription, Settings
│       │   │   ├── state/            # UiState (Loading, Success, Error)
│       │   │   ├── strings/           # Tamil-first localization (Strings)
│       │   │   ├── theme/             # TamixaTheme (Material3, dark/light)
│       │   │   └── viewmodel/         # Auth, Child, Story, Voice, Settings ViewModels
│       │   └── ...
│       ├── androidMain/kotlin/com/tamixa/
│       │   ├── platform/PlatformTime.android.kt
│       │   └── security/TokenStorage.android.kt  # EncryptedSharedPreferences
│       └── iosMain/kotlin/com/tamixa/
│           ├── di/IosPlatformModule.kt          # TokenStorage, PreferencesPort, StoryCache
│           ├── ios/IosApp.kt                     # doInitKoin() entry point for Swift
│           ├── platform/PlatformTime.ios.kt
│           ├── platform/NsUserDefaultsPreferences.kt
│           ├── repository/StoryCacheNsUserDefaults.kt
│           └── security/TokenStorage.ios.kt       # NSUserDefaults (Keychain for prod)
├── androidApp/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/tamixa/android/
│   │   │   ├── TamixaApplication.kt    # Koin init, TokenStorage + sharedModule(baseUrl)
│   │   │   ├── MainActivity.kt        # Compose setContent, TamixaNavHost, FLAG_SECURE callback
│   │   │   ├── navigation/TamixaNavHost.kt  # NavHost, all routes, ViewModels
│   │   │   └── player/AudioPlaybackService.kt  # MediaSessionService + ExoPlayer
│   │   └── res/
│   │       └── values/themes.xml, strings.xml
│   └── ...
└── (iosApp/ when adding iOS target)
```

## Features

1. **Parent auth** – Login / Register; tokens stored securely; token refresh flow.
2. **Child profiles** – List, create; date of birth, language preference.
3. **Story selection** – Cached stories list; navigate to generate or play.
4. **Story generation** – AI request (theme, child, age, language); offline cache of stories.
5. **Audio player** – Placeholder UI; ExoPlayer/MediaSession in `androidApp` for streaming & background.
6. **Voice upload** – Upload voice file; list voice profiles.
7. **Subscription** – Placeholder screen (backend TBD).
8. **Settings** – Language (Tamil first), dark mode toggle, logout.

## Production / Security

- **JWT**: Not stored in plain storage; Android uses EncryptedSharedPreferences.
- **Sensitive screens**: Screenshot prevented on Login/Register via `FLAG_SECURE`.
- **Token refresh**: Ktor Bearer auth with refresh; tokens saved in secure storage.
- **Certificate pinning**: Not yet implemented; use OkHttp engine + `CertificatePinner` in `androidApp` and create Ktor client with that engine for production.
- **Network**: Timeouts, retry (server errors, exponential backoff), JSON content negotiation.
- **Build**: Release minify/shrinkResources and ProGuard rules in place.

## Build

The mobile app uses a single **composeApp** module (Kotlin Multiplatform + Compose) for shared code and Android. Build from the `mobile/` directory (or use `:mobile:composeApp:` from the repo root).

### Android environments (`productFlavors`)

| Flavor | `BuildConfig.BASE_URL` (default) | Override |
|--------|----------------------------------|----------|
| **dev** | `mobile/local.properties` → `TAMIXA_API_BASE_URL`, else `http://10.0.2.2:8080` | Same file: `TAMIXA_WEB_APP_URL` for subscription host |
| **qa** | `https://api-qa.tamixa.com` | `-PTAMIXA_QA_API_BASE_URL=` / `-PTAMIXA_QA_WEB_APP_URL=` |
| **prod** | `https://api.tamixa.com` | `-PTAMIXA_API_BASE_URL=` / `-PTAMIXA_WEB_APP_URL=` |

`BuildConfig` also exposes `TAMIXA_ENVIRONMENT` (`dev` / `qa` / `prod`). Launcher label is **Tamixa (Dev)** / **Tamixa (QA)** for non-prod flavors.

Examples:

```bash
cd mobile

# One debug APK (fast local loop) — dev flavor
./gradlew :composeApp:assembleDevDebug
./gradlew :composeApp:installDebug   # alias → installs devDebug

# QA / prod release (Play-style minified)
./gradlew :composeApp:assembleQaRelease \
  -PTAMIXA_QA_API_BASE_URL=https://your-qa-api.example.com \
  -PTAMIXA_QA_WEB_APP_URL=https://your-qa-app.example.com

./gradlew :composeApp:assembleProdRelease \
  -PTAMIXA_API_BASE_URL=https://api.tamixa.com \
  -PTAMIXA_WEB_APP_URL=https://app.tamixa.com

# All debug or all release variants (slower)
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease
```

APK output names: `tamixa-<variant>.apk` (e.g. `tamixa-devDebug.apk`, `tamixa-prodRelease.apk`) under `composeApp/build/outputs/apk/<flavor>/<buildType>/`.

```bash
# iOS shared framework (Kotlin/Native: Debug or Release only — not Android dev/qa/prod)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:linkReleaseFrameworkIosArm64
```

**Note:** The mobile module has its own `settings.gradle.kts` and Gradle wrapper for standalone builds. From the repo root, use `./gradlew :mobile:composeApp:assembleDevDebug` (same tasks with a `:mobile:` prefix).

`local.properties` for **dev** is resolved as `mobile/local.properties` (monorepo) or `local.properties` next to the mobile root when you open `mobile/` alone.

## iOS Setup

The repo includes **`mobile/iosApp/Tamixa.xcodeproj`**, which runs a script build phase that links the Gradle-built **`shared.framework`** (Kotlin/Native **Debug** or **Release** only). App-level **dev / qa / prod** is selected with **Xcode build configurations** (not separate Gradle flavors).

### iOS environments (Xcode build configurations)

| Configuration | Default API | Default subscription page | Home-screen name |
|---------------|-------------|----------------------------|------------------|
| **DevDebug** / **DevRelease** | `http://127.0.0.1:8080` | `http://127.0.0.1:3000/subscription` | Tamixa (Dev) |
| **QaDebug** / **QaRelease** | `https://api-qa.tamixa.com` | `https://app-qa.tamixa.com/subscription` | Tamixa (QA) |
| **ProdDebug** / **ProdRelease** | `https://api.tamixa.com` | `https://app.tamixa.com/subscription` | Tamixa |

Values come from **target Build Settings** (`TAMIXA_API_BASE_URL`, `TAMIXA_SUBSCRIPTION_WEB_URL`, `TAMIXA_ENVIRONMENT`, `TAMIXA_DISPLAY_NAME`) and are merged into `Info.plist`. The shared **Tamixa** scheme uses **DevDebug** for Run and **ProdRelease** for Archive. Change the run configuration under **Product → Scheme → Edit Scheme… → Run → Build configuration** (e.g. **QaDebug**).

Swift passes plist values into Kotlin via `MainViewController(baseUrl:defaultSubscriptionWebUrl:environment:)`; `IosBuildTimeEnvironment` feeds the subscription default used by `getSubscriptionWebUrl()`.

**Navigation & playback**: `TamixaNavHost` and much playback are Android-oriented; iOS still needs fuller navigation and AVPlayer-based streaming where applicable.

## API Base URL

- **Android:** flavor-specific `BuildConfig.BASE_URL` (see **Android environments**).
- **iOS:** build-configuration-specific `TAMIXA_API_BASE_URL` in Xcode (table above).

Runtime overrides for staging are in **Settings → Server (advanced)**; API override applies after a full app restart.

## Running on a physical device

On a **physical phone/tablet**, `localhost` and `10.0.2.2` point to the device itself, not your dev machine. Use your computer’s **LAN IP** so the app can reach the backend.

1. **Get your machine’s IP** (same Wi‑Fi as the device):
   - **macOS**: System Settings → Network → Wi‑Fi → Details, or run `ipconfig getifaddr en0` (often `192.168.1.x` or `10.0.0.x`).

2. **Backend**: Start the API on your Mac (e.g. `./gradlew :backend:bootRun`). Spring Boot listens on all interfaces by default, so the device can connect.

3. **Backend `.env` (audio streams)**  
   Set the URL the backend returns for audio so the device can load it:
   ```bash
   AUDIO_PUBLIC_BASE_URL=http://YOUR_IP:8080
   ```
   Example: `AUDIO_PUBLIC_BASE_URL=http://192.168.1.5:8080`. Restart the backend after changing.

4. **Android**  
   Create or edit `mobile/local.properties` and add:
   ```properties
   TAMIXA_API_BASE_URL=http://YOUR_IP:8080
   ```
   Optional: `TAMIXA_WEB_APP_URL=http://YOUR_IP:3000` if the subscription web view points to your local web app.  
   Rebuild and install: `./gradlew :composeApp:installDebug` (installs **dev** debug).

5. **iOS**  
   In Xcode: **Target Tamixa → Build Settings**, locate the user-defined keys (filter `TAMIXA_`) for the configuration you run (e.g. **DevDebug**), and set **TAMIXA_API_BASE_URL** to `http://YOUR_IP:8080` (and **TAMIXA_SUBSCRIPTION_WEB_URL** if your local web app is not on port 3000). Rebuild and run on the device.

6. **Firewall**: Ensure your OS/firewall allows incoming TCP connections on port 8080 from the local network.

## Localization

Tamil-first strings in `shared/.../ui/strings/Strings.kt`; language selectable in Settings. Extend `Strings` for more locales.

## Dark Mode

Handled in `TamixaTheme` (system-based); Settings screen has a dark mode toggle (state in `SettingsViewModel`; persist via DataStore/SharedPreferences in app layer if needed).

## Accessibility

Use semantic `contentDescription` on icons and images; support dynamic font scaling and contrast where applicable.

## Firebase / Crashlytics

The app includes Firebase Crashlytics for crash reporting. The `composeApp/google-services.json` file is a **placeholder** with dummy values. You will see logcat warnings (e.g. "Please set a valid API key", "Error getting Firebase installation id") until you replace it—**the app still runs**.

- **To remove the warnings and enable Crashlytics:** see **[FIREBASE_SETUP.md](FIREBASE_SETUP.md)** for step-by-step instructions.
- In **debug** builds, Crashlytics collection is disabled when using the placeholder, so fewer follow-on errors are logged.
- Do not commit a real `google-services.json` with API keys to public repos; use CI secrets or a private config.

## Push Notifications

Firebase BOM and `firebase-messaging-ktx` are included in `androidApp`. Implement FCM handling and token registration in the app module.
