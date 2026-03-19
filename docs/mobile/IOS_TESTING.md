# iOS Testing Guide

Smoke test checklist for the Tamixa mobile app on iOS (Compose Multiplatform).

## Pre-requisites

- Xcode installed (latest stable)
- iOS Simulator or physical device
- Backend running (or mock)

## Build & Run

```bash
# From project root
./gradlew :mobile:composeApp:linkDebugFrameworkIosSimulatorArm64
# Open in Xcode and run on simulator
# Or use: ./gradlew :mobile:composeApp:iosSimulatorArm64Test
```

## Smoke Test Checklist

### Auth
- [ ] Login screen renders
- [ ] OTP flow (if using phone)
- [ ] Passwordless email flow
- [ ] Language selection after first login
- [ ] Navigate to Dashboard after auth

### Dashboard
- [ ] Hero banner displays
- [ ] Category filter (All, Bedtime, etc.)
- [ ] Trending row scrolls
- [ ] Continue Listening row (when playback exists)
- [ ] All Stories list
- [ ] Pull-to-refresh
- [ ] New Story FAB
- [ ] Search icon → Search screen

### Navigation
- [ ] Home (Dashboard)
- [ ] Categories → Story selection
- [ ] Favorites → Favorites list
- [ ] Parent → Settings

### Story Playback
- [ ] Tap story → Audio player
- [ ] Play/Pause
- [ ] Rewind/Forward
- [ ] Progress bar
- [ ] Sleep timer
- [ ] Back exits and saves position

### Search
- [ ] Search field
- [ ] Results from curated + generated
- [ ] Tap result → Audio player

### Favorites
- [ ] List shows favorited stories
- [ ] Toggle favorite (heart) adds/removes
- [ ] Empty state when no favorites

### Settings
- [ ] Language change
- [ ] Voice upload entry
- [ ] Logout

## Platform-Specific Notes

- **TTS**: iOS may use different TTS engine; verify Tamil/Hindi synthesis
- **Images**: Coil/SubcomposeAsyncImage should work; verify placeholder on slow network
- **Pull-to-refresh**: Material3 PullToRefreshBox - verify gesture on iOS
- **Safe area**: Bottom nav and FAB should respect safe area

## Known Limitations

- Some Android-specific code (ExoPlayer) has iOS alternatives; verify audio playback
- Firebase/Crashlytics may differ on iOS
