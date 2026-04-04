# Content ops, web parity, and compliance (runbook)

Short checklist for **curated library** work, **parent-facing web** links, and **safeguards** before regional launch.

## 1. Taxonomy and metadata (content ops)

- Follow **[EDU_METADATA_CONVENTIONS.md](EDU_METADATA_CONVENTIONS.md)** for theme/category and `interactive_graph`.
- **Fun corner:** category **Fun stories** or **Funny Stories** (or matching theme).
- **Learn & safety:** theme/category starts with **Learn** or mentions **Digital Safety**.
- **Practice hub:** **Learn · Simulator** prefix *or* a valid **interactive graph** on the library story (see blueprint below).
- After publishing, spot-check in **admin** and on **web** `/stories?tab=library` with each hub:
  - `hub` omitted or `hub=browse` — full browse + theme chips.
  - `hub=fun` — fun lane only.
  - `hub=learn` — learn + digital safety lane.
  - `hub=simulator` — practice / interactive lane (alias query value matches mobile `Screen.Library.HUB_SIMULATOR`).

## 2. Deep links (support, CRM, onboarding)

| Surface | URL pattern |
|--------|-------------|
| Web library (browse) | `/stories?tab=library` |
| Web Practice hub | `/stories?tab=library&hub=simulator` |
| Web Fun | `/stories?tab=library&hub=fun` |
| Web Learn & safety | `/stories?tab=library&hub=learn` |

Mobile uses the same hub **values** in `library?hub=…` (see `Screen.Library` in the KMP app).

## 3. Compliance copy (web)

- **Settings → Support & safety** pulls strings from `web/src/lib/complianceCopy.ts`. Update that file after legal review; do not paste unverified helpline numbers into fiction or marketing without sign-off.
- Optional regional lines: **`VITE_COMPLIANCE_RESOURCE_LINES`** (see §5 and `web/.env.example`).
- Product rules and India context: **[EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](../EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md)** §10 (safeguards).
- Interactive life-skill summaries: keep parent copy **non-clinical** and **not scores** (aligned with backend copy and mobile).

## 4. Analytics (funnel)

- **Mobile:** `library_hub` app event when the library lane changes (Browse / Fun / Learn / Practice); `interactive_branch` story event when the user picks an interactive choice (stored like other story analytics).
- **Web:** Same events via `POST /api/v1/analytics/app-events` and `.../story-events` (non-blocking on the client).
- Server logs `hubKey` for `library_hub` at DEBUG.

## 5. Compliance env (web)

- Optional **`VITE_COMPLIANCE_RESOURCE_LINES`** — pipe-separated lines for Settings → Support & safety (plain text; URLs become links). Documented in **`.env.example`**. Replace placeholders only after legal sign-off.

## 6. Automated checks (dev)

- Web: `npm test` — `libraryHub` + `storyListenerUi` unit tests.
- Backend: compile / tests after changing `TrackAppEventRequest` / story event types.

## 7. Before launch checklist

- [ ] Helplines / crisis links: **verified** for target region; no implied endorsement of third parties without agreements.
- [ ] At least one **Practice** pilot story visible in `hub=simulator` when catalog slice loads.
- [ ] Privacy/Terms links work from Settings Support section.
- [ ] Spot-test **interactive** playback on web: choice overlay, mission card, **Practice story · choices** chip on player bar.
- [ ] Confirm `library_hub` / `interactive_branch` appear in logs or warehouse (if you add persistence later).

## 8. References

- Simulator authoring: **[EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md](EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md)**
- Product vision (Edu lanes): **[EDUSTORY_PRODUCT_VISION.md](../EDUSTORY_PRODUCT_VISION.md)** §3.9
