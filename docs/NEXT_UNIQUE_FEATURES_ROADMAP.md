# Next Unique Features to Implement (Competitor Differentiation)

**Source:** [UNIQUE_DIFFERENTIATORS.md](UNIQUE_DIFFERENTIATORS.md), [MOBILE_CAPABILITIES_AND_ROADMAP.md](MOBILE_CAPABILITIES_AND_ROADMAP.md), competitor analysis.  
**Purpose:** Ordered list of features to build next so Tamixa stands out vs Epic, FarFaria, TinyTales, etc.

---

## Phase 1 — Surface What Exists & Quick Wins (1–2 sprints)

| # | Feature | Current state | What to implement |
|---|--------|----------------|-------------------|
| **1** | **Listening streak** | Dashboard shows “Build your streak” placeholder; no backend. | **Backend:** Compute consecutive listening days from analytics (story start/complete events per parent). New endpoint e.g. `GET /api/v1/listening/streak` → `{ streakDays: Int, lastListenedAt: Instant? }`. **Mobile:** Pass `streakDays` into Dashboard; replace placeholder with “Your streak: X days” or “Build your streak” when 0. |
| **2** | **Achievements in app** | Backend has `GET /achievements/definitions`; `getByChild` returns empty (child removed). | **Option A:** Add parent-level achievements (e.g. “Listened 5 stories”, “7-day streak”) and `GET /achievements/me` returning earned badges. **Option B:** Show definitions only as “Goals” (no earned state yet). **Mobile:** New screen or bottom-sheet “Achievements” / “Goals” listing definitions (and earned state if backend added). |
| **3** | **Usage on Dashboard** | Usage (e.g. “3/5 stories”) is in Subscription screen; not on Home. | **Mobile:** Dashboard already receives `usageStoriesUsed`, `usageStoriesLimit`; ensure the summary line is prominent (e.g. under greeting). No backend change if data is already passed. |
| **4** | **Moral + parent discussion prompts** | Moral shown in player dialog after story; no discussion prompts. | **Backend:** Add 1–2 discussion prompts per story. Options: (a) New field `discussionPrompts: List<String>` on story (e.g. generated at creation from moral/theme), or (b) LLM endpoint that given `storyId`/moral returns 2 prompts. **Mobile:** In moral dialog (or right after), add section “Talk about it” with 1–2 prompts (e.g. “Ask your child: What would the rabbit do next?”). |

---

## Phase 2 — High Impact, Moderate Effort (2–3 sprints)

| # | Feature | Current state | What to implement |
|---|--------|----------------|-------------------|
| **5** | **Download for offline + Offline library** | Audio is cached on first play; no explicit “Download” or “Offline” UX. | **Mobile:** “Download” on story card or player; mark story as downloaded and cache audio (ExoPlayer cache already exists). Add “Offline” filter/tab or section “Downloaded” that shows only cached stories and works when network is unavailable. **Backend:** Optional `GET /stories/{id}/stream-url` with long cache headers; no change if already streamable. |
| **6** | **Deep link for share** | Share from player uses system intent; no app-openable link. | **Backend:** Short link service or route e.g. `https://tamixa.com/s/{shortId}` that resolves to `storyId` and redirects to app (or store if app not installed). **Mobile:** Register `tamixa://story/{id}` and `https://tamixa.com/s/*`; on open, navigate to story or player. |
| **7** | **Bedtime mode** | Emotion mode (calm/soothing) exists in generation; soundscape and sleep timer exist. | **Mobile:** One-tap “Bedtime” in player or Dashboard: set emotion to calm for next story, auto-enable soundscape, suggest sleep timer (e.g. 15 min). Optional: dimmer UI, “Bedtime stories” collection. **Backend:** Optional “bedtime” collection or filter; no API change if using existing emotion + soundscape. |

---

## Phase 3 — Strong Differentiation (3+ sprints)

| # | Feature | Current state | What to implement |
|---|--------|----------------|-------------------|
| **8** | **“Story from our family” (parent prompt in UI)** | Backend has `parentCustomPrompt` in generation; not exposed in mobile. | **Mobile:** In story generation screen, add optional field: “Add a detail (e.g. ‘Include a dog named Max’ or ‘Set it in Coimbatore’)” → send as `parentCustomPrompt`. **Backend:** Already supports it; ensure safety/sanitization. **Messaging:** Market as “Your family, in the story.” |
| **9** | **Indian folklore / mythology collection** | Categories and themes exist; no dedicated folklore/mythology content or mode. | **Backend:** Themed collection or generation mode: e.g. “Tenali Rama”, “Panchatantra”, “Festival stories (Diwali, Pongal)”. Could be curated library stories + prompt templates for AI. **Mobile:** “Folklore” or “Mythology” in categories; optional “Festival” carousel. |
| **10** | **Parent tips after story** | Moral only. | **Backend:** Optional “parentTip” per story (e.g. “Talk about sharing at dinner”) or one generic tip per theme. **Mobile:** After moral dialog, show one short tip: “Parent tip: Try asking about the moral at bedtime.” |

---

## Phase 4 — Later / Expansion

| # | Feature | Note |
|---|--------|------|
| **11** | **Read along (story text + word highlight)** | Backend returns `wordTimings`; mobile does not render. Add “Read along” tab in player with full text and optional word-by-word highlight. |
| **12** | **Bilingual story mode** | One story mixing two languages (e.g. Tamil + English) for code-switching families. Needs prompt + TTS strategy. |
| **13** | **“Send a story to grandchild”** | Grandparent records or uses clone → shareable link for parent to play for kid. Builds on share clip + deep link + voice clone. |
| **14** | **Parent listening report** | Weekly/monthly summary: “5 stories, 3 new; favorite theme: Adventure.” Backend: aggregate from analytics; Mobile: Settings or email. |
| **15** | **Push notifications** | FCM in project; register token with backend. Notifications: “New story”, “Streak reminder”, “New curated story”. |

---

## Summary: What to Implement Next (Recommended Order)

1. **Listening streak** (backend + Dashboard UI)  
2. **Moral + 1–2 discussion prompts** (backend field or generator + moral dialog UI)  
3. **Usage on Dashboard** (verify and emphasize existing usage line)  
4. **Achievements in app** (parent-level API or definitions-only UI)  
5. **Download for offline + Offline library** (mobile UX + cache)  
6. **Deep link** (backend short link + mobile scheme)  
7. **Bedtime mode** (one-tap in app; emotion + soundscape + timer)  
8. **“Story from our family”** (expose `parentCustomPrompt` in generation screen)

These eight deliver the main differentiators from the competitor analysis; Phases 3–4 add depth and expansion.

---

## Implementation status (recent)

Implemented (excluding Download for offline):

- **Listening streak:** Backend `GET /listening-progress/streak` and `StoryAnalyticsService.getListeningStreakDays()`; mobile fetches via SettingsApi, Dashboard shows "Your streak: X days" or "Build your streak"; refresh on Dashboard load and pull-to-refresh.
- **Moral + discussion prompts:** Moral dialog in player now includes "Talk about it" with two generic prompts (what learned, favorite part); localized (ta, hi, en).
- **Usage on Dashboard:** Already shown under greeting when usage/limit present.
- **Achievements in app:** Backend `GET /achievements/me` (parent-level earned state); mobile AchievementsScreen and Profile menu item "Goals"; Android NavHost updated.
- **Deep link:** Backend `GET /s/{storyId}` redirects to `tamixa://story/{storyId}`. Mobile: register scheme (Android intent-filter / iOS URL types) and navigate to player when app opens from link.
- **Bedtime mode:** "Bedtime story (calm)" FilterChip on StoryGenerationScreen sets `emotionMode = CALM`.
- **"Story from our family":** Optional text field on StoryGenerationScreen ("Add your family to the story"); sent as `parentCustomPrompt` (max 200 chars); localized.

---

## References

- [UNIQUE_DIFFERENTIATORS.md](UNIQUE_DIFFERENTIATORS.md) — Tagline, gaps, prioritization  
- [MOBILE_CAPABILITIES_AND_ROADMAP.md](MOBILE_CAPABILITIES_AND_ROADMAP.md) — Current capabilities and gaps  
- [MOBILE_PRODUCT_EVALUATION.md](MOBILE_PRODUCT_EVALUATION.md) — Competitive comparison
