# Library story metadata — Fun, Edu, and Simulator conventions

**Purpose:** Lock **content taxonomy** so parents and product surfaces can show **Digital Safety** (and future simulators) without mixing them with **Fantasy** or generic Fun. Prevents **content drift** as the team scales.  
**Product context:** [EDUSTORY_PRODUCT_VISION.md](../EDUSTORY_PRODUCT_VISION.md) §3, §3.9; [EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md](../EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md) §8 Phase 1.  
**Authoring (hooks, nodes, fallout, missions):** [EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md](EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md).  
**Ops / web deep links / compliance checklist:** [CONTENT_OPS_WEB_COMPLIANCE_RUNBOOK.md](CONTENT_OPS_WEB_COMPLIANCE_RUNBOOK.md).

---

## 1. Three tracks (how to think)

| Track | Meaning | Typical `theme` (admin “Category” dropdown) |
|-------|---------|---------------------------------------------|
| **Fun** | Entertainment, wonder, bedtime, humor | `Adventure`, `Fantasy`, `Fun stories`, `Funny Stories`, `Moral Stories`, … |
| **Edu (linear)** | Life skills, learning—**no** branching choice engine yet | Any value starting with **`Learn ·`** except **`Learn · Simulator`** |
| **Edu (simulator)** | Interactive branching / choice nodes (when shipped) | `theme` starts with **`Learn · Simulator`** (see §3) |

**`category` field:** Backend may mirror `theme` when unset. Prefer **one primary label on `theme`** for new library rows so mobile filters stay consistent.

---

## 2. Canonical `Learn ·` lanes (editorial)

These strings are **canonical** in code: [StoryCategories.kt](../../backend/src/main/kotlin/com/tamixa/application/storylibrary/StoryCategories.kt), [admin `STORY_CATEGORIES`](../../admin/src/types/api.ts), [mobile `SampleData.categories`](../../mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/data/SampleData.kt).

| Category string | Use for |
|-----------------|--------|
| `Learn · History` | Historical / civic context |
| `Learn · Science & Nature` | STEM curiosity |
| `Learn · Culture & Heritage` | Traditions, identity |
| `Learn · Life Skills` | General habits, social skills (linear) |
| **`Learn · Digital Safety`** | **Scams, UPI safety, forwards, deepfakes—linear Edu** (painkiller lane) |
| **`Learn · Simulator · Digital Safety`** | **Same topics with branching / choice nodes** (when player supports it) |

**Rule:** Do **not** put scam or “customer care” fraud content under **`Fantasy`** or **`Fun stories`**. Use **`Learn · Digital Safety`** (or Simulator variant) so discovery and parent trust stay correct.

---

## 3. Simulator prefix

- **`Learn · Simulator`** is the **machine-detectable** root for interactive life-simulator series.
- Sub-lane after second bullet: **`Learn · Simulator · Digital Safety`**, later e.g. **`Learn · Simulator · Money`**, **`Learn · Simulator · Conflict`** (keep **≤ 100** characters; align naming with product before adding new rows to `StoryCategories`).

**Mobile:** `isSimulatorStory(theme, category)` in [StoryListenerUi.kt](../../mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/StoryListenerUi.kt) — use for a future **Edu Simulator** hub without affecting Fun.

---

## 4. Fun lane (quick reference)

**Fun corner** in the app treats **`Fun stories`** and **`Funny Stories`** (case-insensitive on theme/category) as the light lane—see `isFunStory` in `StoryListenerUi.kt`. Other non-`Learn` categories are general library Fun/entertainment unless product defines otherwise.

---

## 5. Implementation checklist (ops)

1. **New safety content:** tag **`Learn · Digital Safety`** until branching ships; then migrate pilot to **`Learn · Simulator · Digital Safety`**.
2. **Bulk generation:** only use categories from the canonical list (admin bulk UI).
3. **Custom / legacy strings:** backend `StoryCategories.toCanonical` maps known aliases; unknown strings still save—avoid one-off typos like `Learn-` without `·`.

---

## 6. Critical path (engineering + narrative)

These are **make-or-break** for the simulator MVP; keep them in sprint reviews.

1. **Next-segment latency:** Pre-fetch or cache the **next audio asset** while the choice overlay is visible so playback resumes in **&lt; ~1s** after tap—not 5s cold start ([EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md](../EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md) §10 Phase A).
2. **Theme ↔ graph (server):** If `interactive_graph` JSON contains a **non-empty `segments`** object, the API rejects saves unless **theme or category** matches **`Learn · Simulator`** (same rule as admin lint). Empty `{}` or `{}` segments do not trigger this.
3. **Family mission tone:** Missions are **optional challenges** (“spot one too-good-to-be-true sign today”), not **homework** or graded tasks ([EDUSTORY_PRODUCT_VISION.md](../EDUSTORY_PRODUCT_VISION.md) §3.9.1).
4. **Decision nodes:** **Wrong options must look tempting** (e.g. “₹500 cashback”) so the lesson is **judgment**, not obvious multiple-choice. Hire or contract **interactive narrative design** for pilot scripts ([EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md](../EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md) §10 implementation checklist).

---

## 7. Interactive graph JSON (`library_stories.interactive_graph`)

Stored as **TEXT** (valid JSON). Edited in **admin → story → Interactive episode** or via API on `CreateLibraryStoryRequest.interactiveGraph`.

**Shape:**

```json
{
  "startSegmentId": "intro",
  "overlayStyle": "WHATSAPP_CHAT",
  "segments": {
    "intro": {
      "audioUrl": "https://cdn.example.com/pilot/intro.mp3",
      "choices": [
        {
          "id": "tap_link",
          "label": "Open the payment link",
          "nextSegmentId": "bad_path",
          "skillDeltas": { "DIGITAL_WISDOM": -1 }
        },
        {
          "id": "ignore",
          "label": "Delete the chat",
          "nextSegmentId": "safe_path",
          "skillDeltas": { "DIGITAL_WISDOM": 1 }
        }
      ]
    },
    "bad_path": { "audioUrl": "https://...", "choices": [] },
    "safe_path": { "audioUrl": "https://...", "choices": [] }
  }
}
```

- **`audioUrl`:** Absolute `https` URL or app-resolvable audio path (same rules as other library audio URLs).
- **`choices`:** Empty array = end of branch; show **post-episode mission** if `post_story_mission` / `post_story_resource_url` are set.
- **`skillDeltas`:** Optional; recorded via parent `POST /api/v1/edu/life-skill-choices` when a child profile is available (no aggregate scores returned yet).

**`skillDeltas` → four pillars (normalization):** Author keys are folded server-side into **wisdom / social / money / balance** (soft counters, not grades). Examples:

| Author key (examples) | Pillar |
|------------------------|--------|
| `DIGITAL_WISDOM`, `wisdom`, keys ending in `_wisdom` | wisdom |
| `social`, `social_capital`, `empathy`, `negotiation` | social |
| `money`, `fiscal_muscle`, `financial` | money |
| `balance`, `cognitive_agency`, `focus`, `research` | balance |

Parents may **`GET /api/v1/edu/life-skill-choices/counters?childId=`** for raw pillar integers plus disclaimer copy.

---

## 8. Revision

When adding a **new canonical category**, update **all three**: `StoryCategories.kt`, `admin/src/types/api.ts` `STORY_CATEGORIES`, `SampleData.kt` (order: keep `All` first in mobile only; server list has no `All`).
