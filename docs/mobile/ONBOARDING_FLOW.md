# Tamixa High-Retention Onboarding Flow

This document defines the first-launch onboarding flow for the mobile app.

## Flow (Post-Install)

```
Install
    ↓
Hook Screen
    "Stories told by voices you love"
    ↓
Demo Story (~20 sec)
    talking character, subtitles, narration
    ↓
Voice invitation (optional)
    "Stories in your family voice?" — continue or skip
    ↓
Avatar invitation (optional)
    "Who should tell the story?" — upload photo or skip
    ↓
Login (soft gate)
    Save progress • Clone voice • Upload avatar (post-auth)
```

## Screen Details

| Step | Screen | Purpose |
|------|--------|---------|
| 1 | **Hook** | Emotional hook — communicate Tamixa's unique value immediately |
| 2 | **Demo** | Magic moment — short story with talking character, subtitles, narration |
| 3 | **Voice invitation** | Optional — value prop for family voice; skip completes onboarding and goes to Login |
| 4 | **Avatar invitation** | Optional — value prop for talking avatar; skip/continue completes onboarding → Login |
| 5 | **Login** | Soft gate — auth before Dashboard; voice/avatar flows continue in-app |

**Removed from onboarding (no longer in NavHost):** story interests picker, home preview card, bedtime reminder screen. Child interests and themes are handled outside this flow (e.g. child profile, story generation). Bedtime notification infrastructure (`OnboardingReminderPort`, preferences) may be reused from Settings later.

## Implementation Notes

- **Onboarding state**: `hasCompletedOnboarding` stored in PreferencesPort; runs once per install.
- **Auth**: Login required to reach Dashboard; "soft" = friendly copy, not punitive.
- **Skip**: Hook can skip to Login (complete onboarding); Voice/Avatar steps offer Skip → Login with onboarding completed.
- **Bedtime reminder**: Not shown during onboarding. `OnboardingReminderPort` + preference keys remain on Android/iOS for a future Settings entry or push-based reminder.

## Related

- [MOBILE_CAPABILITIES_AND_ROADMAP.md](../MOBILE_CAPABILITIES_AND_ROADMAP.md)
- [SCREEN_TIME_REMINDER_PLAN.md](../SCREEN_TIME_REMINDER_PLAN.md) — notification patterns
