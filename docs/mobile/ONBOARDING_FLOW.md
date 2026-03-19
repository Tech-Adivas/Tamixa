# Tamixa High-Retention Onboarding Flow

This document defines the first-launch onboarding flow for the mobile app.

## Flow (Post-Install)

```
Install
    ↓
Hook Screen
    "Stories told by voices you love"
    ↓
Demo Story (20 sec)
    talking character, subtitles, narration
    ↓
Choose Story Interests
    animals / friendship / adventure (select 2–3)
    ↓
Home Screen Preview
    what they'll see: Continue, Recommended, Popular, Categories
    ↓
"Remind me at bedtime"
    optional notification for Day-1 retention
    ↓
Login Prompt (soft)
    Save progress • Clone voice • Upload avatar
```

## Screen Details

| Step | Screen | Purpose |
|------|--------|---------|
| 1 | **Hook** | Emotional hook — communicate Tamixa's unique value immediately |
| 2 | **Demo** | Magic moment — 20s story with talking character, subtitles, narration |
| 3 | **Interests** | Personalization — build recommendation profile (Animals, Adventure, Friendship, Village Life, Funny) |
| 4 | **Home Preview** | Set expectations — show Continue, Recommended, Popular, Categories |
| 5 | **Bedtime Reminder** | Day-1 retention — "Remind me at bedtime" notification option |
| 6 | **Login** | Soft gate — save progress, clone voice, upload avatar |

## Implementation Notes

- **Onboarding state**: `hasCompletedOnboarding` stored in PreferencesPort; runs once per install.
- **Preferred themes**: Stored as comma-separated list; used for home screen recommendations.
- **Bedtime reminder**: Platform-specific (OnboardingReminderPort); default 8:00 PM.
- **Auth**: Login required to reach Dashboard; "soft" = friendly copy, not punitive.
- **Skip**: Hook has "Skip"; other steps use "Continue" to keep momentum.

## Related

- [MOBILE_CAPABILITIES_AND_ROADMAP.md](../MOBILE_CAPABILITIES_AND_ROADMAP.md)
- [SCREEN_TIME_REMINDER_PLAN.md](../SCREEN_TIME_REMINDER_PLAN.md) — notification patterns
