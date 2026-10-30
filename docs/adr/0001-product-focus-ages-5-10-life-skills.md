# ADR-0001: Product focus — ages 5–10 and life-skills stories

## Status

Proposed — awaiting leadership sign-off (see Approval below).

## Date

2026-10-23

## Context

Tamixa has grown in several directions at once: stories for toddlers through adults (the admin "Age group" list ran
from 1–2 years to 61–99 years), AI-generated stories, cloned family voices, interactive "Learn · Simulator" stories,
life-skill tracking, and parent avatar videos (HeyGen / SadTalker / D-ID / Gooey adapters). A small team cannot make
all of these excellent, and the Play Store plan needs a clear audience for the Families declaration.

Evidence from the product today:

- The strongest content is narrated Tamil stories with a moral, plus interactive stories where the child makes
  choices; life-skill choices are already tracked (`LifeSkillChoiceService`, life-skill pillars, admin analytics).
- Library ages are unreliable: 20 of 25 published stories carry the seed default age 10 and others carry ages up to 72,
  so the app had no meaningful age filter.
- Avatar video is the most expensive and riskiest feature (paid third-party video APIs, parent photo = biometric-style
  data, long generation times) and is already off by default (`AVATAR_VIDEO_ENABLED=false`).

## Decision

1. **Audience:** Tamixa targets children **aged 5–10**, through their parents. Content, UX copy and Play Store
   target-audience settings follow this range.
2. **Content focus:** stories that build **life skills** (feelings, honesty, sharing, friendship, safety, everyday
   independence), narrated and interactive.
3. **Age bands:** the app groups listeners into **5–6, 7–8 and 9–10**. Parents pick a band on Home or Library; the
   choice filters library rows and recommendations and is remembered on the device. "All ages" stays available.
4. **Avatar video moves out of the current plan.** It stays switched off and is kept as a possible **later premium
   option**; no new work on it until this ADR is revisited.

## Consequences

### Positive

- One clear audience for content, marketing and the Play Families / Data Safety declarations.
- Parents of a 5-year-old and a 10-year-old see different, more suitable stories.
- No spend or privacy exposure from avatar video in the launch phase.

### Negative / trade-offs

- Stories outside 5–10 are hidden whenever a band is selected (still visible under "All ages").
- Every published story must be re-checked and given the right age group in admin; until then the filter reflects the
  old ages (e.g. 20 seed stories at age 10 appear only under 9–10).
- Avatar video code remains in the codebase (disabled) and has to be kept compiling.

### Follow-ups

- Content team: set the correct age group on every published story (admin → story → Age group).
- Product: set Play Console target age groups to 5–8 and 9–12 to match (see the Play Store / privacy work).
- Revisit avatar video as a premium add-on after launch metrics are in.

## Implementation (2026-10-23)

- App: `AgeBand` (`mobile/composeApp/src/commonMain/kotlin/com/tamixa/domain/AgeBand.kt`), age picker on Home and
  Library, filter in `StoryViewModel`, saved via `PreferencesPort.getStoryAgeBand`; story generation age limited to
  5–10.
- Admin: "Age group" offers only 5–6 / 7–8 / 9–10 (older values stay visible on edit so they are not lost).
- Backend: no change — library stories already carry `age`, which admin sets to the band's first year.

## Approval

| Role | Name | Decision | Date |
|------|------|----------|------|
| Leadership | | | |

## References

- Roadmap: [docs/ROADMAP.md](../ROADMAP.md)
- Avatar video design (parked): [docs/AVATAR_VIDEO.md](../AVATAR_VIDEO.md)
- Compliance: [docs/COMPLIANCE.md](../COMPLIANCE.md)
