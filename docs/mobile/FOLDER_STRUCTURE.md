# Tamixa Mobile – Folder Structure

Current structure uses a single **composeApp** module (KMP: Android, iOS).

```
mobile/
├── build.gradle.kts
├── settings.gradle.kts            # include :composeApp
├── gradle.properties
├── README.md
├── FOLDER_STRUCTURE.md
│
└── composeApp/
    ├── build.gradle.kts           # KMP: android, iosX64, iosArm64, iosSimulatorArm64
    └── src/
        ├── commonMain/kotlin/com/tamixa/
        │   ├── application/port/   # PreferencesPort, etc.
        │   ├── di/                 # KoinInit, SharedModule, ViewModelModule
        │   ├── domain/             # ModelStory, etc.
        │   ├── network/            # ApiConfig, HttpClientFactory, StoryApi, AuthApi, KtorEngine
        │   ├── repository/         # StoryRepository, AuthRepository, etc.
        │   ├── navigation/         # TamixaNavHost
        │   ├── player/             # StoryPlaybackController
        │   ├── platform/           # PlayerControllerFactory (expect/actual)
        │   ├── util/               # TamixaConstants, TamixaLog, PlatformLog
        │   └── ui/
        │       ├── strings/        # Strings.kt (localization)
        │       ├── theme/          # Theme.kt
        │       ├── screen/         # *Screen (Login, Dashboard, AudioPlayer, etc.)
        │       ├── components/     # StoryCard, TamixaHeroBanner, etc.
        │       ├── viewmodel/      # *ViewModel
        │       └── TamixaApp.kt
        ├── androidMain/kotlin/com/tamixa/
        │   ├── android/            # MainActivity, TamixaApplication, player, component
        │   ├── platform/           # DataStorePreferences, PlayerControllerFactory, PlatformApi
        │   ├── network/            # KtorEngine.android
        │   └── util/               # PlatformLog.android
        └── iosMain/kotlin/com/tamixa/
            ├── ios/               # IosApp, component (AvatarVideoSurfaceIos, FamilyVoiceRecordDialogIos)
            ├── platform/          # NsUserDefaultsPreferences, PlayerControllerFactory, PlatformApi
            ├── network/           # KtorEngine.ios
            └── util/              # PlatformLog.ios
```

## Navigation (routes)

- `login` → Login
- `register` → Register
- `children` → Child list
- `children/create` → Add child
- `stories` → Story selection
- `stories/generate` → Generate story
- `audio/{storyId}` → Audio player
- `voice` → Voice upload
- `subscription` → Subscription
- `settings` → Settings (language, theme, logout)

## API

- Base URL: `ApiConfig` / build config.
- Endpoints: `/api/v1/auth/*`, `/api/v1/children`, `/api/v1/stories/*`, `/api/v1/voice/*`.
- Auth: Bearer from secure storage; refresh on 401.
