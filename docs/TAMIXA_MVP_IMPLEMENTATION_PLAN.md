# Tamixa MVP Implementation Plan

Gap analysis and implementation tasks for aligning the mobile app with the [Tamixa MVP Screen Blueprint](TAMIXA_MVP_SCREENS.md).

---

## Current vs Blueprint

### Screens Comparison

| Blueprint Screen | Current Implementation | Status |
|-----------------|------------------------|--------|
| **1. Splash** | `SplashScreen` | ✅ Implemented (logo, tagline, animation) |
| **2. Hook** | `OnboardingHookScreen` | ✅ Implemented (headline, Start Story Magic, Skip) |
| **3. Demo Story** | `OnboardingDemoScreen` | ⚠️ Partial – static placeholder, no playable 20‑sec demo |
| **4. Story Preferences** | — | ❌ Not in onboarding (removed `OnboardingInterestsScreen`). Themes/preferences: `StoryGenerationScreen`, child profile / recommendations as applicable |
| **5. Voice Recording** | `VoiceUploadScreen` | ✅ Implemented – optional onboarding invite → `OnboardingVoiceInvitationScreen` → full flow post-login |
| **6. Avatar Upload** | `AvatarUploadScreen` | ✅ Implemented – optional onboarding invite → `OnboardingAvatarInvitationScreen` → full flow post-login |
| **7. Home** | `DashboardScreen` | ✅ Implemented (greeting, continue, recommended, categories) |
| **8. Story Library** | `StorySelectionScreen`, `SearchScreen` | ✅ Exists – no dedicated tab |
| **9. Story Player** | `AudioPlayerScreen` | ✅ Implemented (character, subtitle, controls) |
| **10. Voice Selection** | In `AudioPlayerScreen` | ✅ Voice dropdown in player |
| **11. Talking Avatar Video** | In `AudioPlayerScreen` | ✅ Avatar video when backend provides URL |
| **12. Create Story** | `StoryGenerationScreen` | ✅ Implemented |
| **13. Profile** | `ProfileScreen` (+ `MyVoiceAndAvatarScreen`, `SettingsScreen` via nav) | ✅ Unified Profile hub with links to voice, settings, etc. |
| **14. Share Story** | In `AudioPlayerScreen` | ✅ Share action in player |

### Navigation & Bottom Bar

| Blueprint | Current |
|-----------|---------|
| Home \| Library \| Create \| Profile | Home \| Library \| Fun & learn \| Profile *(Create via dashboard/library; voice/avatar & settings under Profile)* |

---

## Gap Summary

1. **Demo Story** – Onboarding demo is static; blueprint expects a playable ~20‑sec story with talking character and subtitles.
2. **Story Library tab** – No bottom‑bar tab for Library; access is via Search / categories.
3. **Profile hub** – Largely addressed via `ProfileScreen`; remaining polish is tab ordering vs blueprint.
4. **Optional onboarding flow** – Blueprint: Demo → [Preferences] → [Voice] → [Avatar] → Home. **Current:** Hook → Demo → Voice invitation → Avatar invitation → **Login** (`OnboardingInterestsScreen` and `OnboardingBedtimeReminderScreen` removed; home preview step not used). Theme/child interests are not collected in onboarding; bedtime reminder prefs + `OnboardingReminderPort` remain for future Settings or notifications.
5. **Bottom bar** – Blueprint: Home \| Library \| Create \| Profile. **Current:** Home \| Library \| Fun & learn \| Profile (Create lives inside flows; My voice & avatar and Settings open from Profile).

---

## Implementation Tasks

### Phase 1: Documentation & Alignment

- [x] **TASK-001** – Create `docs/TAMIXA_MVP_SCREENS.md`
- [x] **TASK-002** – Create `docs/TAMIXA_MVP_IMPLEMENTATION_PLAN.md`

### Phase 2: Profile Hub & Navigation

- [x] **TASK-003** – Add `ProfileScreen` as unified hub
  - Sections: user name, My Voices, My Avatars, Favorite Stories, Listening History, Premium Subscription, Settings
  - Reuse `MyVoiceAndAvatarScreen`, `SettingsScreen`, `FavoritesScreen`, `SubscriptionScreen` via navigation
  - File: `mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/screen/ProfileScreen.kt`

- [x] **TASK-004** – Update bottom bar to support Profile
  - Option A: Add Profile tab → Home \| Library \| My voice & Avatar \| Profile (Settings moves under Profile)
  - Option B: Match blueprint → Home \| Library \| Create \| Profile (larger change)
  - Update `TamixaBottomBar.kt` and `TamixaTab` enum

- [x] **TASK-005** – Add Library tab
  - New tab that navigates to `StorySelectionScreen` or a dedicated Library view
  - Route: `Screen.StorySelection` or `Screen.Library`

### Phase 3: Demo Story Enhancement

- [x] **TASK-006** – Make onboarding demo playable
  - Embed or stream a ~20‑sec demo story
  - Talking character (or avatar placeholder), subtitle, play/pause
  - May require demo asset(s) and backend support

### Phase 4: Optional Onboarding Steps

- [x] **TASK-007** – Add optional Voice invitation after Demo
  - After Demo, show “Want stories in your family voice?” with Record / Skip
  - Skip → continues to Avatar invitation or Login (per NavHost)

- [x] **TASK-008** – Add optional Avatar invitation after Voice (or after Demo if Voice skipped)
  - “Who should tell the story?” with Upload Photo / Skip

### Phase 5: Polish & Content

- [ ] **TASK-009** – Add “Listening History” section to Profile
  - Link to recent playback (reuse `recentPlaybackWithStories` data)

- [x] **TASK-010** – Ensure Profile shows user name
  - Use `authViewModel` or settings to display display name (e.g. “Achappan”)

---

## Suggested PR Breakdown

| PR | Scope | Tasks |
|----|-------|-------|
| PR-1 | Profile hub | TASK-003, TASK-009, TASK-010 |
| PR-2 | Bottom bar & Library | TASK-004, TASK-005 |
| PR-3 | Demo story enhancement | TASK-006 |
| PR-4 | Optional onboarding | TASK-007, TASK-008 |

---

## Files to Modify

| File | Changes |
|------|---------|
| `mobile/.../Screen.kt` | Add `Profile`, `Library` (if new route) |
| `mobile/.../TamixaNavHost.kt` | Wire Profile, Library; optional onboarding routes |
| `mobile/.../TamixaBottomBar.kt` | Add/update tabs per chosen option |
| `mobile/.../ProfileScreen.kt` | New file |
| `mobile/.../OnboardingDemoScreen.kt` | Playable demo (TASK-006) |
| `mobile/.../TamixaApp.kt` | No changes if using existing NavHost |

---

## Out of Scope for MVP

- Share Story as standalone screen (in‑player share is enough)
- Full Create Story voice input (optional feature)
- Talking Avatar Video as separate screen (handled in player)
