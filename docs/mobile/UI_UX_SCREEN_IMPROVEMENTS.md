# UI/UX Screen-Level Improvements — Investigation

**Purpose:** Screen-by-screen gaps and recommendations (must-have vs good-to-have).  
**Reference:** [UI_UX_DESIGN_ANALYSIS.md](./UI_UX_DESIGN_ANALYSIS.md)

---

## Summary

| Priority | Count | Focus |
|----------|--------|--------|
| **P1 (Must have)** | 8 | Accessibility, error/retry, confirmations, touch targets |
| **P2 (Good to have)** | 12 | Consistency (tokens), empty states, loading semantics, section clarity |
| **P3 (Polish)** | 6 | Animations, haptics, reduced motion |

---

## By screen

### SplashScreen
- **P2:** Replace hardcoded `padding(20.dp)`, `size(120.dp)`, `size(240.dp)` with `TamixaDesignTokens.screenPadding` and named sizes where it improves consistency.
- **P3:** Optional subtle scale/fade on logo (already has delay).

### Onboarding (Hook, Demo, Voice, Avatar, Interests, HomePreview, Bedtime)
- **P1:** Ensure every tappable control (Skip, Continue, chips) has at least 48dp touch target; FilterChip/Button are usually OK—verify custom taps.
- **P2:** Unify card padding: use `TamixaDesignTokens.cardContentPadding` (20.dp) instead of mixed 16.dp/14.dp/12.dp on cards.
- **P3:** Entrance animations already present; consider respecting “reduce motion” later.

### LoginScreen
- **P2:** Replace `padding(24.dp)` with `TamixaDesignTokens.screenPadding` for horizontal padding on terms block.
- **P2:** Ensure OTP/passwordless error state is visible and retry is obvious (e.g. “Try again” or re-send code).

### RegisterScreen
- **P1:** Add semantics to loading spinner: `Modifier.semantics { contentDescription = Strings.loading() }`.
- **P2:** Consider explicit “Retry” near the error card (user can also tap Register again—acceptable).

### LanguageSelectionScreen
- **P2:** Use `TamixaDesignTokens.screenPadding` for outer padding if currently hardcoded.
- **P1:** Already fixed: Check/Arrow icons have contentDescription (selected/select).

### DashboardScreen
- **P2:** Empty state (“No stories yet”) already has illustration + copy; ensure CTA (“Tap New Story”) is clearly the primary action.
- **P3:** Hero carousel and FAB already have contentDescription; no change needed.

### LibraryScreen / StorySelectionScreen
- **P2:** Replace `padding(24.dp)` with `TamixaDesignTokens.screenPadding` for content area.
- **P2:** Story list item padding `padding(12.dp)` → `TamixaDesignTokens.cardContentPadding` (or keep 12.dp for compact list and document as intentional).
- **P1:** Loading/error/retry already added.

### SearchScreen
- **P1:** Add **loading state** while search is in progress (e.g. show `CircularProgressIndicator` or skeleton when `searchQuery.length >= 2` and results not yet received); otherwise users may think nothing is happening.
- **P1:** Add **error state + retry** if search API fails (snackbar is OK; optional inline “Something went wrong. Retry”).
- **P2:** Empty state already has emoji + “No results” + hint; consider `TamixaChildrenListeningIllustration` for consistency with other empty states.
- **P2:** TamixaEmojiDisplay("🔍") — add `contentDescription = Strings.noSearchResults()` or similar for a11y.

### StoryGenerationScreen
- **P1:** Loading bar already has semantics (Strings.loading()).
- **P2:** Surface/card padding 24.dp → `TamixaDesignTokens.cardContentPadding` for consistency.
- **P2:** Ensure “Generate” is disabled with clear reason when at limit (e.g. “Upgrade to create more”).

### AudioPlayerScreen
- **P1:** `AsyncImage` for storytelling avatar has `contentDescription = null` — use e.g. `Strings.yourAvatar()` or “Storyteller avatar”.
- **P2:** Play/pause and other controls already have contentDescription; verify rewind/forward labels are clear.
- **P2:** Replace hardcoded `padding(24.dp)`, `padding(16.dp)` with tokens where it’s screen-level content padding.

### ProfileScreen
- **P1:** Menu item images already use `contentDescription = label`.
- **P2:** Loading/error/retry already present.

### SettingsScreen
- **P1:** **Settings load error + retry:** If initial settings load fails, show an error message and “Retry” button (today only loading spinner is shown; on failure user may see incomplete content).
- **P2:** Logout/delete confirmations already added.
- **P2:** Consent/export cards already use `TamixaDesignTokens`; listening progress card idem.
- **P3:** Spacer(Modifier.padding(8.dp)) → Spacer(Modifier.height(8.dp)) if it’s vertical spacing (consistency).

### SubscriptionScreen
- **P1:** **Subscription load error + retry:** When `loadSubscription()` fails, show inline error + “Retry” (currently only loading state; failure may leave screen empty or stale).
- **P2:** Cards already use `cardRadiusLarge`, `cardContentPadding`; BenefitRow a11y fixed.
- **P2:** Bottom padding 32.dp — if screen is shown with bottom nav, use `TamixaDesignTokens.screenPaddingBottomWithNav`; else keep 32.dp.

### VoiceUploadScreen
- **P2:** Voice “no stories yet” and profiles empty state use cards; consider small illustration (e.g. TamixaMascot or emoji) for consistency with Favorites/Listening history empty states.
- **P2:** Replace remaining `padding(20.dp)` with `TamixaDesignTokens.cardContentPadding` on cards.
- **P1:** Loading and error states present; ensure error card has retry or clear “Try again” path.

### AvatarUploadScreen
- **P1:** Delete button icon: `contentDescription = null` → `Strings.removeAvatar()` (a11y).
- **P2:** “No stories with avatar” empty state: text-only card; consider adding illustration for consistency.
- **P2:** Card padding 20.dp/24.dp → `TamixaDesignTokens.cardContentPadding` where applicable.
- **P2:** Use `TamixaDesignTokens.cardRadiusLarge` for main cards instead of `dialogRadius` if design spec prefers.

### MyVoiceAndAvatarScreen
- **P1:** Mic icon already has contentDescription.
- **P2:** Avatar card uses emoji 📖; optional contentDescription on the card for “Add avatar” (card is clickable).

### FavoritesScreen / ListeningHistoryScreen
- **P1:** Loading/error/retry already implemented.
- **P2:** Empty states have illustration + copy; no change required unless copy is updated.

### Components (shared)
- **TamixaHeroBanner:** Carousel image `contentDescription = null` — use story title or “Story cover” for the visible item.
- **StoryCoverImage:** Placeholder/fallback already use `contentDescription = story.theme`; ensure AsyncImage in player has avatar description (see AudioPlayerScreen).
- **TamixaMascot / TamixaEmojiDisplay:** Decorative; `contentDescription = null` is OK or use “Decorative” to skip in a11y.

---

## Cross-cutting

### Consistency (tokens)
- **Screen padding:** Prefer `TamixaDesignTokens.screenPadding` (24.dp) for main content horizontal/top padding instead of raw `24.dp`, `20.dp`.
- **Card padding:** Prefer `TamixaDesignTokens.cardContentPadding` (20.dp) inside cards; use `cardRadius` / `cardRadiusLarge` instead of `dialogRadius` for list/content cards where appropriate.
- **Section spacing:** Use `TamixaDesignTokens.sectionSpacing` (24.dp) between sections.

### Accessibility
- **Icons:** Every `Icon`/`Image` that is interactive or meaningful (not purely decorative) should have a non-null `contentDescription` (localized via `Strings`).
- **Progress indicators:** `LinearProgressIndicator` / `CircularProgressIndicator` used for “loading” should have `Modifier.semantics { contentDescription = Strings.loading() }` (or equivalent).

### Error + retry
- **Screens that load data once:** Settings, Subscription, Search (on submit) — should show loading, and on failure show error message + Retry (or snackbar + retry on next open). Today: Settings and Subscription do not show explicit error UI; Search does not show loading.

### Destructive actions
- Logout: confirmation added.
- Delete account: confirmation present.
- Cancel subscription: confirmation present.
- No other destructive actions identified.

---

## Suggested implementation order

1. **P1 (quick):** AvatarUploadScreen Delete icon contentDescription; AudioPlayerScreen avatar AsyncImage contentDescription; SearchScreen loading state; Settings and Subscription error + retry UI.
2. **P2 (batch):** Replace hardcoded padding/sizes with tokens on 2–3 high-traffic screens (Login, StorySelection, Subscription bottom padding where applicable); add SearchScreen error + retry if API can fail; TamixaHeroBanner carousel contentDescription.
3. **P3:** Optional empty-state illustrations (Voice/Avatar), reduce-motion, haptics—as capacity allows.
