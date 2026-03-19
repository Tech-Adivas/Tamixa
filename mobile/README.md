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

The mobile app uses a single **composeApp** module (Kotlin Multiplatform + Compose) for shared code and Android. Build from the `mobile/` directory:

```bash
cd mobile

# Android debug
./gradlew :composeApp:assembleDebug

# Android release
./gradlew :composeApp:assembleRelease

# iOS framework (for Xcode integration)
./gradlew :composeApp:linkReleaseFrameworkIosArm64
```

To run on a connected device or emulator:
```bash
./gradlew :composeApp:installDebug
```

**Note:** The mobile module has its own `settings.gradle.kts` and Gradle wrapper for standalone builds. Building from the project root with `./gradlew :mobile:composeApp:assembleDebug` may have compatibility constraints; prefer building from `mobile/` for reliable results.

Set `BASE_URL` in `composeApp` `build.gradle.kts` (BuildConfig) to your API base URL.

## iOS Setup

The `composeApp` module builds a **shared** framework for iOS with platform ports (TokenStorage, PreferencesPort, StoryCache), Koin init, and shared UI code. To complete the iOS app:

1. **Create an Xcode project** (or use Compose Multiplatform’s iOS app template) and add the `shared` framework.
2. **Call Koin init** from Swift in your app launch:
   ```swift
   IosAppKt.doInitKoin(baseUrl: "http://127.0.0.1:8080")  // Simulator
   ```
3. **Host Compose UI** in a `UIViewController` using `ComposeUIViewController()` from the Compose framework.
4. **Navigation & playback**: `TamixaNavHost` and audio/video playback are Android-specific (ExoPlayer, Media3). For full iOS parity, you need to:
   - Add multiplatform navigation (or an `iosMain`-specific NavHost)
   - Implement AVPlayer-based playback controllers for streaming, TTS, and avatar video
   - Add `FamilyVoiceRecordDialog` using AVAudioRecorder
   - Configure background audio via AVAudioSession

## API Base URL

Defaults to `ApiConfig.DEFAULT_BASE_URL`; override via `BuildConfig.BASE_URL` in the Android app (see `TamixaApplication` and `build.gradle.kts`).

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
   Rebuild and install: `./gradlew :composeApp:installDebug`.

5. **iOS**  
   In `iosApp/Tamixa/Info.plist`, set `TAMIXA_API_BASE_URL` to your machine’s URL, e.g.:
   ```xml
   <key>TAMIXA_API_BASE_URL</key>
   <string>http://192.168.1.5:8080</string>
   ```
   Rebuild the app in Xcode and run on the device.

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
