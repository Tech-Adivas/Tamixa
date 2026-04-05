# Digital Survival Guide — Episode 1 graph, blueprint check, pilot script bank

**Theme (canonical):** `Learn · Simulator · Digital Safety`  
**Series:** The Digital Survival Guide  
**Use:** Paste the JSON into **Admin → Library story → Interactive episode** after uploading audio to your CDN. Replace `https://cdn.tamixa.app/...` URLs with real assets.

**Database (library_stories):** Flyway **[V86](../../backend/src/main/resources/db/migration/V86__digital_survival_guide_script_seed.sql)** inserts **20 DRAFT** rows (Ep01 × 6 locales + English outlines Ep02–15). **[V87](../../backend/src/main/resources/db/migration/V87__digital_survival_guide_ep02_kyc_graph.sql)** **`UPDATE`s Ep02 (en)** (KYC). **[V88](../../backend/src/main/resources/db/migration/V88__digital_survival_guide_ep03_ep05_graphs.sql)** **`UPDATE`s Ep03–Ep05 (en)**. **[V89](../../backend/src/main/resources/db/migration/V89__digital_survival_guide_ep06_ep08_graphs.sql)** **`UPDATE`s Ep06–Ep08 (en)**. **[V90](../../backend/src/main/resources/db/migration/V90__digital_survival_guide_ep09_ep15_graphs.sql)** **`UPDATE`s Ep09–Ep15 (en)** (PAN/UPI, refund remote, matrimonial APK, tipster, society portal, hospital secrecy). Shared shape for Ep03–Ep15: `epNN_hook` → `epNN_mid_1|2|3` → `epNN_outcome_bad|safe|report`. `story_owner` = `seed:digital-survival-guide-v1`. Regenerate: `python3 backend/scripts/generate_digital_survival_guide_seed_sql.py` (V86–V90 + `digital_survival_guide_ep01_en` … `ep15_en` `.graph.json` in `backend/src/test/resources/edu/`). Ops: **[CONTENT_OPS_WEB_COMPLIANCE_RUNBOOK.md](CONTENT_OPS_WEB_COMPLIANCE_RUNBOOK.md)** §9.

---

## App surfacing (mobile, web, parent API)

No extra “DSG-only” UI was added. Once a row is **deliverable** through the normal library pipeline, clients pick it up like any other interactive library story.

| Layer | Behavior |
|--------|-----------|
| **Mobile — Library → Practice** | Tab **`LibraryHubTab.Simulator`** filters cached library rows with **`isInteractivePracticeLibraryStory`**: theme/category matches **`Learn · Simulator…`** (regex in code) **or** the API returned a non-empty **`interactiveGraph`**. Implementation: [`StoryListenerUi.kt`](../../mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/StoryListenerUi.kt) (`isSimulatorStory`, `isInteractivePracticeLibraryStory`), hub wiring in [`LibraryScreen.kt`](../../mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/screen/LibraryScreen.kt). Entry from home: [`TamixaNavHost.kt`](../../mobile/composeApp/src/commonMain/kotlin/com/tamixa/navigation/TamixaNavHost.kt) (`onNavigateToLibrarySimulator` → `HUB_SIMULATOR`). |
| **Branching playback** | When the user opens a library story that has a graph, navigation uses **`parseInteractiveStoryGraph(story?.interactiveGraph)`** (same nav host). |
| **Web — Stories** | **Practice** lane uses the same ideas (`isSimulatorStory` / interactive graph helpers in [`Stories.tsx`](../../web/src/pages/Stories.tsx)). |
| **Parent API gate** | **`GET /api/v1/stories/library`** and **`GET .../{id}`** only return stories **approved for delivery** with **playable audio** for the requested language (`narrationApprovedAt`, `audioFileUrl`). See [`LibraryStoryController.kt`](../../backend/src/main/kotlin/com/tamixa/api/library/LibraryStoryController.kt) and **`findByLanguageApprovedOnly`**. **DRAFT** Flyway seeds **do not** appear in the parent app until publish + narration + approval as usual. |

**Metadata check:** Canonical theme **`Learn · Simulator · Digital Safety`** satisfies the mobile **Learn · Simulator** prefix rule, so DSG titles appear in **Practice** after they are API-visible.

### Local dev — end-to-end without CDN or admin publish

Spring **`dev`** profile only (default on a typical laptop). Config: **`app.digital-survival-dev`** in [`application-dev.yml`](../../backend/src/main/resources/application-dev.yml).

1. Run Postgres and apply Flyway **V86–V90**.
2. Call **`POST /api/v1/dev/digital-survival/prepare-e2e-seed`** (no auth; dev profile already permits `/api/v1/dev/**`). This **publishes** every row with **`story_owner = seed:digital-survival-guide-v1`**, sets **`narration_approved_at`**, placeholder **`audio_file_url`**, and ensures **`story_translations`** + **`story_narration_audio`** (default voice, **READY**) so the same JPA rules as real narration jobs are satisfied.
3. Parent library **`GET /api/v1/stories/library`** and **`GET .../{id}`** post-process responses: segment **`audioUrl`** values under **`https://cdn.tamixa.app/library/sim/digital-survival-guide/`** are rewritten to **`/api/v1/dev/digital-survival/placeholder.mp3`**, which is served from the repo (**classpath** MP3).
4. Mobile: API base must reach the backend (e.g. Android emulator **`10.0.2.2:8080`**). Set catalog language to **`en`** to load all English pilots; use **`ta`**, **`hi`**, etc. for the matching Ep01 locale rows.
5. In the app: **Library → Practice** → open a DSG story → branching playback uses the placeholder for every segment until you ship real assets.

**Staging / production:** Keep flags **off** (see root `application.yml`). Use real CDN MP3s, normal approval, and admin publish. Non-**`dev`** profiles **deny** `/api/v1/dev/**`.

**Code:** [`DigitalSurvivalDevController.kt`](../../backend/src/main/kotlin/com/tamixa/api/dev/DigitalSurvivalDevController.kt), [`DigitalSurvivalDevE2eSeedService.kt`](../../backend/src/main/kotlin/com/tamixa/application/dev/DigitalSurvivalDevE2eSeedService.kt), [`DevDigitalSurvivalParentLibraryPostProcessor.kt`](../../backend/src/main/kotlin/com/tamixa/api/dev/DevDigitalSurvivalParentLibraryPostProcessor.kt); parent API wiring in [`LibraryStoryController.kt`](../../backend/src/main/kotlin/com/tamixa/api/library/LibraryStoryController.kt).

---

## 1. Blueprint alignment (Episode 1 — “The Midnight Blackout”)

Cross-check against [EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md](EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md).

| Blueprint step | Your script | Fit / note |
|----------------|-------------|------------|
| **1. Relatable hook** | Ramesh at home, fan/tea, daughter working; SMS at 8 PM | Strong India-specific domestic frame; co-listening works. |
| **2. Dilemma (node)** | Call / official app / ask Priya | Three stances match Category 1 table (panic / verify / collaborate). Options feel defensible in the moment. |
| **3. Narrative fallout** | A: remote-support app trap; B: app shows paid; C: Priya spots sender + grammar | **Show** consequences on A; relief + learning on B/C. Avoid saying “you failed” — use narrator stress + pattern hint on A. |
| **4. Reflection / mission** | Pattern alerts + “Screen-Share Challenge” SMS audit | Optional, non-homework tone per [EDU_METADATA_CONVENTIONS.md](EDU_METADATA_CONVENTIONS.md) §6. |

**Category 1 (digital wisdom):** Teaches **urgency**, **unknown channel**, **small-amount lure**, **tooling** (screen share) — aligns with blueprint § “pattern recognition.”

**Revision notes (narrative continuity):**

- After **B** or **C**, add one line in audio that a **new** SMS or call arrives (“same threat, different number”) so the ₹10 beat does not ignore what they already proved.
- After **A**, a line like “verification did not go through on our side — pay ₹10 on this link” bridges into the shared second node without softening the A-path lesson.

**Overlay:** `CARDS` reads neutrally for SMS context; `WHATSAPP_CHAT` is also valid (see [InteractiveStoryOverlays.kt](../../mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/edu/InteractiveStoryOverlays.kt)).

---

## 2. `interactive_graph` JSON — Episode 1 (admin-ready)

Three mid-episode branches each include **Part 2 + Part 3** in one audio file, then present **Decision Node 2**. Three terminal outcomes after Node 2.

```json
{
  "startSegmentId": "ep01_hook",
  "overlayStyle": "CARDS",
  "segments": {
    "ep01_hook": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hook_to_node1.mp3",
      "choices": [
        {
          "id": "call_sms_number",
          "label": "Call the number in the SMS immediately",
          "nextSegmentId": "ep01_mid_panic_call",
          "skillDeltas": { "DIGITAL_WISDOM": -2, "balance": -1 }
        },
        {
          "id": "open_official_app",
          "label": "Open the official electricity app or website to check",
          "nextSegmentId": "ep01_mid_verify_app",
          "skillDeltas": { "DIGITAL_WISDOM": 2, "balance": 1 }
        },
        {
          "id": "ask_family_review_sms",
          "label": "Ask Priya to read the SMS carefully together",
          "nextSegmentId": "ep01_mid_collaborate",
          "skillDeltas": { "DIGITAL_WISDOM": 1, "balance": 1 }
        }
      ]
    },
    "ep01_mid_panic_call": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/branch_call_plus_fee_trap.mp3",
      "choices": [
        {
          "id": "pay_ten_fee",
          "label": "Pay ₹10 on the link — small amount, stay safe",
          "nextSegmentId": "ep01_outcome_pin_trap",
          "skillDeltas": { "DIGITAL_WISDOM": -3 }
        },
        {
          "id": "refuse_upi_on_link",
          "label": "Stop — never enter UPI PIN on a link from SMS",
          "nextSegmentId": "ep01_outcome_safe",
          "skillDeltas": { "DIGITAL_WISDOM": 2, "balance": 1 }
        },
        {
          "id": "report_and_block",
          "label": "Report to the real helpline and block the number",
          "nextSegmentId": "ep01_outcome_report",
          "skillDeltas": { "DIGITAL_WISDOM": 3, "balance": 1 }
        }
      ]
    },
    "ep01_mid_verify_app": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/branch_app_plus_fee_trap.mp3",
      "choices": [
        {
          "id": "pay_ten_fee",
          "label": "Pay ₹10 on the link — small amount, stay safe",
          "nextSegmentId": "ep01_outcome_pin_trap",
          "skillDeltas": { "DIGITAL_WISDOM": -3 }
        },
        {
          "id": "refuse_upi_on_link",
          "label": "Stop — never enter UPI PIN on a link from SMS",
          "nextSegmentId": "ep01_outcome_safe",
          "skillDeltas": { "DIGITAL_WISDOM": 2, "balance": 1 }
        },
        {
          "id": "report_and_block",
          "label": "Report to the real helpline and block the number",
          "nextSegmentId": "ep01_outcome_report",
          "skillDeltas": { "DIGITAL_WISDOM": 3, "balance": 1 }
        }
      ]
    },
    "ep01_mid_collaborate": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/branch_collab_plus_fee_trap.mp3",
      "choices": [
        {
          "id": "pay_ten_fee",
          "label": "Pay ₹10 on the link — small amount, stay safe",
          "nextSegmentId": "ep01_outcome_pin_trap",
          "skillDeltas": { "DIGITAL_WISDOM": -3 }
        },
        {
          "id": "refuse_upi_on_link",
          "label": "Stop — never enter UPI PIN on a link from SMS",
          "nextSegmentId": "ep01_outcome_safe",
          "skillDeltas": { "DIGITAL_WISDOM": 2, "balance": 1 }
        },
        {
          "id": "report_and_block",
          "label": "Report to the real helpline and block the number",
          "nextSegmentId": "ep01_outcome_report",
          "skillDeltas": { "DIGITAL_WISDOM": 3, "balance": 1 }
        }
      ]
    },
    "ep01_outcome_pin_trap": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/outcome_what_if_pin.mp3",
      "choices": []
    },
    "ep01_outcome_safe": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/outcome_safe_family.mp3",
      "choices": []
    },
    "ep01_outcome_report": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/outcome_reported.mp3",
      "choices": []
    }
  }
}
```

**Audio production map**

| File (suggested) | Covers |
|------------------|--------|
| `hook_to_node1` | Part 1 hook through Decision Node 1 (pause before choices). |
| `branch_call_plus_fee_trap` | Choice A fallout + bridge + fake officer ₹10 + pause before Node 2. |
| `branch_app_plus_fee_trap` | Choice B fallout + **new message/call** + ₹10 + pause. |
| `branch_collab_plus_fee_trap` | Choice C fallout + **new message/call** + ₹10 + pause. |
| `outcome_what_if_pin` | “What if” PIN-harvest lesson (simulator-safe tone). **Full line-by-line VO:** §5 `ep01_outcome_pin_trap` per locale. |
| `outcome_safe_family` | Refused SMS link; calm family beat + habit line. **Full VO:** §5 `ep01_outcome_safe`. |
| `outcome_reported` | Block/report + official channel habit. **Full VO:** §5 `ep01_outcome_report`. |

**Post-story mission (admin text field):**  
“The Screen-Share Challenge: Sit with parents or grandparents; open one unknown SMS together; name urgency, sender, grammar; delete together.”

**Episode 2 (KYC) — audio map** (English graph in **V87**; path `.../digital-survival-guide/ep02/en/`)

| File (in graph) | Covers |
|-----------------|--------|
| `hook_to_node1` | Hook through Decision Node 1. |
| `branch_tap_link_plus_fee_trap` | Tap-link path + ₹2 “gateway” trap + pause before Node 2. |
| `branch_bank_app_plus_fee_trap` | Real app shows no KYC pending + follow-up SMS + Node 2. |
| `branch_card_call_plus_fee_trap` | Card-helpline path + same Node 2 beat. |
| `outcome_otp_harvest` | OTP / fake-form “what if” lesson. |
| `outcome_safe_family` | Refused SMS payment; trusted channel. |
| `outcome_reported` | Official line + block/report habit. |

**Episodes 3–15 (V88–V90, English)** — CDN `.../digital-survival-guide/epNN/en/` with `NN` zero-padded (`ep03` … `ep15`). Shared segment ids: `epNN_hook` → `epNN_mid_1|2|3` → `epNN_outcome_bad|safe|report`. **V88** = Ep03–Ep05; **V89** = Ep06–Ep08; **V90** = Ep09–Ep15. Filenames are in each migration and matching test fixtures `digital_survival_guide_ep03_en.graph.json` … `ep15_en.graph.json`.

---

## 3. Pilot script bank (15 episodes) — same model

Each entry: **hook → Node 1 (A/B/C) → branch lesson → Node 2 (converge) → mission.**  
**Patterns to voice in narrator asides:** urgency/secrecy, small rupee ask, wrong channel, impersonation of trust (sarkari / bank / school).

### Episode 2 — “The KYC Countdown”

- **Hook:** SMS: “HDFC/SBI — KYC incomplete, account blocked in 2 hours. Link to update.”  
- **Node 1:** A) Tap link now · B) Open bank app only · C) Call number printed on **debit card** back.  
- **Fallout:** A → fake form + OTP harvest; B/C → no pending KYC in real app.  
- **Node 2:** “Pay ₹2 gateway charge to unfreeze.” A) Pay · B) Never pay via SMS link · C) Call official 1800 from card / statement.  
- **Mission:** Find the **one official** customer-care number for your bank (statement/card) and save it as a contact.  
- **DB / graph:** English seed row gets a full **`interactive_graph`** from Flyway **V87** (segment IDs `ep02_hook` → three mids → `ep02_outcome_*`; CDN path `.../digital-survival-guide/ep02/en/*.mp3`). Extend **Ep02** to other locales by duplicating the row or adding `story_translations` when narrations exist.

### Episode 3 — “The ₹49 Courier”

- **Hook:** “BlueDart — your parcel on hold; ₹49 customs digitization fee.”  
- **Node 1:** A) Pay ₹49 · B) Check tracking on **brand site** with AWB · C) Ask family “did anyone order?”  
- **Converge:** Second SMS: “Download tracking APK.”  
- **Node 2:** A) Install APK · B) Never install APK from SMS · C) Call courier from **website** number only.  
- **Mission:** Compare sender ID on a real courier SMS vs a random 10-digit sender.  
- **DB / graph:** English pilot row updated by **V88** (`ep03_*` segments).

### Episode 4 — “The School Panic Call”

- **Hook:** Voice call: “I am from your child’s school; accident; send money for ambulance UPI.”  
- **Node 1:** A) Send immediately · B) Hang up, call **school landline** you already have · C) Call spouse first, conference.  
- **Node 2:** “Don’t tell teachers — secrecy fee ₹100.” A) Pay · B) Secrecy = red flag · C) Conference with class teacher on known number.  
- **Mission:** Agree a **family codeword** for real emergencies (no shame in verifying).  
- **DB / graph:** English pilot row updated by **V88** (`ep04_*` segments).

### Episode 5 — “The Job Registration Fee”

- **Hook:** WhatsApp: “TCS/Infosys walk-in confirmed; pay ₹499 registration.”  
- **Node 1:** A) Pay for “sure seat” · B) Check **careers.** official site only · C) Ask elder who works in IT.  
- **Node 2:** “Upgrade to fast-track ₹99.” A) Pay · B) Real employers don’t charge registration · C) Report number.  
- **Mission:** List **three** signs a job offer is fake (money upfront, personal Gmail, urgency).  
- **DB / graph:** English pilot row updated by **V88** (`ep05_*` segments).

### Episode 6 — “The Lottery You Never Entered”

- **Hook:** “Congratulations — KBC / Google winner ₹5 lakh; pay ₹1 verification.”  
- **Node 1:** A) Pay ₹1 · B) Delete — you never entered · C) Search scam pattern with family.  
- **Node 2:** “Add admin on Telegram for prize OTP.” A) Add · B) OTP = account key · C) Block and mark spam.  
- **Mission:** One dinner question: “What would you never do for ‘free’ money?”  
- **DB / graph:** English pilot row updated by **V89** (`ep06_*` segments).

### Episode 7 — “The Subsidy Portal”

- **Hook:** SMS: “PM Ujjwala / subsidy — register on this portal before midnight.”  
- **Node 1:** A) Register on link · B) Check **mygov / official** channel news · C) Ask panchayat / gas agency next visit.  
- **Node 2:** “Upload Aadhaar front-back on this form.” A) Upload · B) Never upload Aadhaar to random sites · C) Use only **official** UIDAI flows if ever needed.  
- **Mission:** Point out **one** fake “.com” domain vs a known `.gov.in` / `.nic.in` pattern (age-appropriate).  
- **DB / graph:** English pilot row updated by **V89** (`ep07_*` segments).

### Episode 8 — “Insurance Lapse Tonight”

- **Hook:** Call: “Your LIC policy lapsed; pay ₹500 now or lose ₹2 lakh benefit.”  
- **Node 1:** A) Pay on link · B) Open **policy document** / agent number you chose yourself · C) Visit branch next working day.  
- **Node 2:** “Share screen so I can fill the form.” A) Share · B) No insurer asks Anydesk · C) Hang up, call **policy** helpline.  
- **Mission:** Locate your **real** policy number and helpline on paper; put in one drawer.  
- **DB / graph:** English pilot row updated by **V89** (`ep08_*` segments).

### Episode 9 — “PAN–Aadhaar Last Day”

- **Hook:** Urgent SMS with “income tax” logo image — link to link PAN “or fine ₹10,000.”  
- **Node 1:** A) Panic-link · B) Open **incometax.gov.in** from typed URL · C) Ask CA / literate family member.  
- **Node 2:** “Pay ₹100 processing on UPI.” A) Pay · B) Government portals don’t collect random ₹100 on SMS · C) Verify deadline on **official** site only.  
- **Mission:** Practice typing **one** government URL together (no tap from message).  
- **DB / graph:** English pilot row updated by **V90** (`ep09_*` segments).

### Episode 10 — “The Accidental UPI Transfer”

- **Hook:** “I sent ₹5000 to your number by mistake; please return to this UPI ID.”  
- **Node 1:** A) Return immediately · B) Check if money **actually credited** in app · C) Ignore — common script.  
- **Node 2:** “Send ₹1 test first.” A) Send · B) Test payments still train you to trust scammer · C) If real credit, use **bank dispute** channel, not stranger ID.  
- **Mission:** Read aloud: “UPI **received** vs **promised**.”  
- **DB / graph:** English pilot row updated by **V90** (`ep10_*` segments).

### Episode 11 — “The Refund Executive”

- **Hook:** “Amazon/Flipkart refund failed; download TeamViewer for verification.”  
- **Node 1:** A) Download · B) Open **app orders** only — no refund pending · C) Ask teen to verify order ID.  
- **Node 2:** “Type refund amount in this remote box.” A) Type · B) Never type amounts/PIN for a ‘refund’ · C) Use in-app help chat only.  
- **Mission:** Show **one** real in-app “help” path on a parent phone.  
- **DB / graph:** English pilot row updated by **V90** (`ep11_*` segments).

### Episode 12 — “The Rishta Verification”

- **Hook:** Matrimonial site DM: “Pay ₹999 for police verification badge.”  
- **Node 1:** A) Pay for trust · B) Check site **settings / verified** rules on official FAQ · C) Discuss with family offline first.  
- **Node 2:** “Video KYC on this APK.” A) Install · B) APK outside Play Store = high risk · C) Meet through arranged family channel only.  
- **Mission:** List **two** green flags (known introducer, slow pace) vs red flags (money before meeting).  
- **DB / graph:** English pilot row updated by **V90** (`ep12_*` segments).

### Episode 13 — “The Telegram Tipster”

- **Hook:** “Join VIP group — bank Nifty sure shot; pay ₹50 admin fee.”  
- **Node 1:** A) Pay to join · B) Remember: no one sells **sure** returns · C) Talk to parent about investing only via SEBI-registered advisors.  
- **Node 2:** “Deposit ₹500 in this UPI for ‘margin’.” A) Deposit · B) Stop — pyramid pattern · C) Exit group, report.  
- **Mission:** One line: “If they need **your** money to teach **you** money, walk away.”  
- **DB / graph:** English pilot row updated by **V90** (`ep13_*` segments).

### Episode 14 — “Society Maintenance Portal”

- **Hook:** WhatsApp from “Secretary” unknown number: “Pay maintenance on new portal link.”  
- **Node 1:** A) Pay on link · B) Check **society notice board** / known secretary number · C) Ask neighbor on known phone.  
- **Node 2:** “Late fee ₹20 if not paid in 10 minutes.” A) Pay · B) Real societies rarely minute-level threaten · C) Confirm in **elevator group** with known admins.  
- **Mission:** Save **two** trusted society contacts (not just “Secretary” label).  
- **DB / graph:** English pilot row updated by **V90** (`ep14_*` segments).

### Episode 15 — “The Hospital Bed Deposit” (sensitive — tone: calm, no graphic detail)

- **Hook:** Call claiming hospital needs **instant** deposit for a named relative.  
- **Node 1:** A) Transfer now · B) Call relative on **saved** number · C) Call hospital **main line** from Google Maps / known bill.  
- **Node 2:** “Don’t tell family — they’ll worry.” A) Obey secrecy · B) Secrecy + urgency = scam signature · C) Conference call with family.  
- **Mission:** Agree: “Any **real** hospital bill can wait **one** verified phone call.”  
- **DB / graph:** English pilot row updated by **V90** (`ep15_*` segments).

---

## 4. Metadata quick reference

| Field | Value |
|-------|--------|
| **theme** | `Learn · Simulator · Digital Safety` |
| **Internal series** | The Digital Survival Guide |
| **Simulator nodes** | 2 per episode (unless you simplify to 1 for early MVP) |
| **skillDeltas keys** | e.g. `DIGITAL_WISDOM`, `balance` (see [EDU_METADATA_CONVENTIONS.md](EDU_METADATA_CONVENTIONS.md) §7) |

For new episodes, duplicate the JSON structure: one **hook** segment, **three** mid segments (or fewer if you cut a branch), **three** terminal outcomes after the second node — or adjust to your writer’s fork count; lint requires every `nextSegmentId` to exist in `segments`.

---

## 5. Episode 1 — Multilingual line scripts (En · Hi · Ta · Te · Kn · Ml)

**Episode title (working):** The Midnight Blackout / அரையிரவு மின் துண்டிப்பு / आधी रात बिजली गुल …  
**Cast:** Narrator; **Ramesh** (senior); **Priya** (daughter); **Fake Officer** (calm, “customer care” — not cartoon villain).  
**Segment map:** Lines below follow §2 files: **`hook_to_node1`** → **`branch_*`** → **`outcome_*`**. SFX in square brackets for the sound team.

**Utility board names (say aloud in VO):** En generic “official electricity app”; **Hi** बिजली विभाग; **Ta** TANGEDCO (டான்ஜெட்கோ); **Te** విద్యుత్ శాఖ / డిస్కాం అధికారిక యాప్; **Kn** BESCOM / ಕರ್ನಾಟಕ ವಿದ್ಯುತ್ ಉತ್ತಮ; **Ml** KSEB.

---

### 5.1 English (En) — India, co-listening

**[SFX: crickets, ceiling fan, tea cup]**

**Narrator:** Ramesh is finally unwinding. His daughter Priya is in the next room, finishing office work. His phone buzzes.

**Ramesh:** *(muttering)* Who’s texting at eight in the evening?

**[SFX: notification]**

**Ramesh:** Priya! Look at this. It says our electricity will be cut tonight because the “last bill wasn’t updated.” “Click here to avoid blackout.”

**Priya:** *(distracted)* Papa, ignore it… wait, did we pay the bill?

**Ramesh:** I think we did… but what if I forgot? It’s forty degrees. If the fan stops, we won’t sleep. There’s a number here — “call the electricity officer now.”

**— Decision Node 1 (UI labels)**  
A) Call the number in the SMS now · B) Open the **official** electricity app or website · C) Ask Priya to read the SMS together

**Branch A — after choice**

**[SFX: ring, quick pickup]**  
**Fake Officer:** Electricity board helpline. Sir, your bill is pending. Disconnection in thirty minutes. Please download the “support app” from this link so I can verify your payment.  
**Ramesh:** Okay, okay, I’m doing it…  
**Narrator:** Pause. Ramesh almost gave a stranger remote access. **Pattern alert:** real staff don’t rush you to install screen-sharing “support” apps.  
**Fake Officer:** Verification didn’t sync on my side. Just pay **ten rupees** on this link — procedural fee — and your connection will be safe.

**Branch B — after choice**

**Ramesh:** Let me check the official app first.  
**[SFX: app taps]**  
**Ramesh:** Wait… it says **Paid.** No dues. Then why this message?  
**Narrator:** That SMS was a phantom — fear, not facts.  
**[SFX: second notification]**  
**Narrator:** A **new** message arrives — same story, different number.  
**Fake Officer (SMS voiceover or short call):** Sir, pay **ten rupees** verification on this link or tonight’s disconnection will proceed.

**Branch C — after choice**

**Priya:** Papa, look at the sender. It’s a ten-digit mobile number — not a short code like a real utility. And the grammar… “your account is block.” This is a scam.  
**Narrator:** Priya used two clues: **who** sent it, and **how** it’s written.  
**[SFX: second notification]**  
**Fake Officer:** Final notice — pay **ten rupees** on this link to confirm payment.

**— Decision Node 2 (UI labels)**  
A) Pay ₹10 — it’s small, safer to comply · B) Stop — never enter **UPI PIN** on a link from SMS · C) Report to the **real** helpline and block the number

**— `ep01_outcome_pin_trap` (full VO — “what if” / teachable moment)**  
**Narrator:** Suppose Ramesh had tapped “pay ten rupees” and typed his **UPI PIN** on that page.  
**[SFX: soft tension under the VO — not horror]**  
**Narrator:** That screen was a **copy**. It was not sending ten rupees to the electricity board — it was built to **capture** the PIN. Accounts can be drained in seconds; sometimes thieves take a tiny amount first so you don’t panic immediately.  
**Priya:** Papa!  
**Narrator:** Here in the simulator you picked the risky option **on purpose** — to **see** the pattern. In real life: **one** deep breath, **one** check on the **official** app or the number printed on your **real** bill — and the trap stays shut. **Pattern:** small rupee, big damage — it was never about ten rupees.

**— `ep01_outcome_safe` (full VO)**  
**Ramesh:** I’m not putting my UPI PIN into some link from a message.  
**[SFX: browser/app close, soft exhale]**  
**Priya:** Good. Our bill already shows **paid** in the official app.  
**Narrator:** The “only ten rupees” line works because it sounds harmless. The risk isn’t the amount — it’s **who** sees what you type. Real payments belong in the **UPI app you already trust**, or on a **website you typed yourself** — not a stranger’s page opened from SMS.  
**Ramesh:** Fan’s still on. We’re alright.  
**Narrator:** Digital wisdom is not “never feel afraid.” It’s **slowing the hand** before the finger taps **Pay**.

**— `ep01_outcome_report` (full VO)**  
**Priya:** I’ll screenshot this SMS — for the cyber-crime portal or the police helpline, whatever Mum uses.  
**Ramesh:** I’m blocking this number **now**.  
**[SFX: block / mute confirm]**  
**Narrator:** Block and report. You don’t just protect yourselves — you add noise to the scammer’s business. They buy new numbers, but complaints and blocks still **add up**.  
**Ramesh:** Next time I’ll open the **official** channel **before** I panic.  
**Narrator:** That’s the habit: **trusted channel first** — stranger’s link, never.

**Family mission (end card):** Today, with a parent or grandparent, open one unknown SMS that says “click link.” Name **urgency**, **sender**, **grammar**. Delete it together.

---

### 5.2 Hindi (Hi)

**[SFX: ठीक वही]**  

**Narrator:** रमेश जी आराम कर रहे हैं। बेटी प्रिया दूसरे कमरे में ऑफिस का काम कर रही है। अचानक फोन बजता है।

**Ramesh:** अरे… रात आठ बजे कौन मैसेज?

**[SFX: नोटिफिकेशन]**  

**Ramesh:** प्रिया! देखो… लिखा है आज रात बिजली काट देंगे, “लास्ट बिल अपडेट नहीं हुआ।” “ब्लैकआउट से बचने के लिए यहाँ क्लिक करें।”

**Priya:** पापा, इग्नोर करो… रुको, बिल भरा था ना?

**Ramesh:** शायद भरा… पर भूल तो नहीं गया? बाहर तो चालीस डिग्री। पंखा बंद हुआ तो नींद ही नहीं आएगी। यहाँ नंबर भी है — “तुरंत बिजली विभाग के ऑफिसर को कॉल करें।”

**— नोड 1 (चुनाव टेक्स्ट)**  
A) SMS वाले नंबर पर तुरंत कॉल करूँ · B) सरकारी / आधिकारिक बिजली ऐप या वेबसाइट खोलकर चेक करूँ · C) प्रिया से SMS साथ मिलकर ध्यान से पढ़वाऊँ

**शाखा A**  

**[SFX: फोन]**  
**Fake Officer:** नमस्ते, बिजली विभाग हेल्पडेस्क। सर, आपका बिल पेंडिंग है। तीस मिनट में कनेक्शन कटेगा। वेरिफिकेशन के लिए इस लिंक से “सपोर्ट ऐप” डाउनलोड कर लीजिए।  
**Ramesh:** हाँ हाँ, करता हूँ…  
**Narrator:** रुकिए। अजनबी को फोन का रिमोट कंट्रोल मिल जाता है। **पैटर्न:** असली अफसर Anydesk / TeamViewer जैसी स्क्रीन-शेयरिंग के लिए जल्दबाज़ी नहीं करते।  
**Fake Officer:** हमारे सिस्टम में सिंक नहीं हुआ। बस इस लिंक से **दस रुपये** वेरिफिकेशन फीस भर दीजिए — कनेक्शन सुरक्षित रहेगा।

**शाखा B**  

**Ramesh:** पहले आधिकारिक ऐप देख लूँ।  
**[SFX: टैप]**  
**Ramesh:** रुको… “पेड” दिखा रहा है। बकाया नहीं। फिर यह मैसेज क्यों?  
**Narrator:** डर का फैंटम मैसेज — सच नहीं।  
**[SFX: दूसरा मैसेज]**  
**Narrator:** दूसरा SMS आता है — वही धमकी, नया नंबर।  
**Fake Officer:** सर, **दस रुपये** इस लिंक पर भरें, नहीं तो आज रात कटौती।

**शाखा C**  

**Priya:** पापा, भेजने वाले का नाम देखो — दस अंकों का मोबाइल; सरकारी शॉर्ट कोड जैसा नहीं। और लिखावट — “आपका अकाउंट ब्लॉक” जैसी गलतियाँ। ये स्कैम है।  
**Narrator:** दो सुराग: **किसने** भेजा, **कैसे** लिखा।  
**[SFX: दूसरा मैसेज]**  
**Fake Officer:** अंतिम नोटिस — इस लिंक पर **दस रुपये** भुगतान करें।

**— नोड 2**  
A) दस रुपये छोटी रकम है, भर देता हूँ · B) रुकूँगा — SMS के लिंक पर **UPI PIN** कभी नहीं · C) असली हेल्पलाइन पर रिपोर्ट करूँगा और नंबर ब्लॉक करूँगा

**— `ep01_outcome_pin_trap` (पूरा VO)**  
**Narrator:** मान लीजिए रमेश जी ने “दस रुपये” पर टैप करके **UPI PIN** डाल दिया।  
**[SFX: हल्का तनाव — डरावना नहीं]**  
**Narrator:** वो पेज नकली था। दस रुपये बिजली विभाग को नहीं जा रहे थे — PIN चुराने के लिए बनाया गया था। खाता सेकंडों में खाली हो सकता है; कभी पहले एक छोटी रकम काटकर देखते हैं ताकि आपको शक न हो।  
**Priya:** पापा!  
**Narrator:** सिम्युलेटर में आपने जानबूझकर खतरनाक विकल्प चुना — **ताकि पैटर्न दिखे।** असल ज़िंदगी में: एक गहरी साँस, **असली बिल** या **आधिकारिक ऐप** से एक बार चेक — और जाल खाली रह जाता है। **पैटर्न:** छोटी रकम, बड़ा नुकसान — मामला दस रुपये का नहीं था।

**— `ep01_outcome_safe` (पूरा VO)**  
**Ramesh:** मैं किसी SMS के लिंक पर अपना **UPI PIN** नहीं डालूँगा।  
**[SFX: ऐप बंद / साँस]**  
**Priya:** सही। हमारे **आधिकारिक ऐप** में तो पहले से “पेड” दिख रहा है।  
**Narrator:** “बस दस रुपये” इसलिए चलता है क्योंकि लगता है — छोटी बात। असली खतरा रकम नहीं, **कौन** आपकी उंगलियाँ देख रहा है। असली भुगतान: जिस **UPI ऐप** पर भरोसा है, या **खुद टाइप** की वेबसाइट — SMS से खुले अजनबी पेज पर नहीं।  
**Ramesh:** पंखा चल रहा है। हम ठीक हैं।  
**Narrator:** डिजिटल समझदारी का मतलब “कभी डर न लगे” नहीं — **उंगली उठने से पहले रुकना**।

**— `ep01_outcome_report` (पूरा VO)**  
**Priya:** मैं इस SMS का स्क्रीनशॉट लेती हूँ — साइबर हेल्पलाइन / पोर्टल के लिए।  
**Ramesh:** मैं इस नंबर को **अभी** ब्लॉक करता हूँ।  
**[SFX: ब्लॉक कन्फर्म]**  
**Narrator:** ब्लॉक और रिपोर्ट — सिर्फ आप नहीं, **अगले पड़ोसी** की भी रक्षा। नंबर बदलते हैं, पर शिकायतें और ब्लॉक **जमा** होते हैं।  
**Ramesh:** अगली बार घबराने से पहले **असली चैनल** खोलूँगा।  
**Narrator:** आदत यही: **पहले भरोसेमंद चैनल** — अजनबी का लिंक, कभी नहीं।

**मिशन:** आज दादा-दादी या माता-पिता के साथ एक “अनजान नंबर” वाला SMS खोलें। **जल्दबाज़ी**, **भेजने वाला**, **भाषा** — तीन लाल झंडे बताएँ। साथ में डिलीट करें।

---

### 5.3 Tamil (Ta)

**Narrator:** ரமேஷ் சார் இளைப்பாறுகிறார். மகள் பிரியா அடுத்த அறையில் அலுவலக வேலை. மொபைல் ஒலிக்கிறது.

**Ramesh:** ஐயோ… இரவு எட்டு மணிக்கு யார் மெசேஜ்?

**[SFX: அறிவிப்பு]**  

**Ramesh:** பிரியா! பார்… இன்று இரவு மின்சாரம் வெட்டுவோம் என்கிறார்கள், “கடைசி பில் அப்டேட் ஆகல.” “பவர் கட் தவிர இங்கே கிளிக்.”

**Priya:** அப்பா, புறக்கணி… நில்லு, பில் கட்டினோமா?

**Ramesh:** கட்டினோம் நினைக்கிறேன்… மறந்துட்டேனோ? வெளியே நாற்பது டிகிரி. பங்கா நின்னா தூக்கமே வராது. இங்கே நம்பர் — “உடனே TANGEDCO அதிகாரியை கால் பண்ணுங்க.”

**— நோடு 1**  
A) SMS-ல இருக்க நம்பருக்கு உடனே கால் · B) TANGEDCO / அதிகாரிக் **அதிகாரப்பூர்வ** ஆப் அல்லது வலைத்தளம் திறந்து சரிபார்ப்பு · C) பிரியாவை கூப்பிட்டு SMS ஒண்ணா படித்து பார்க்க வைக்கிறேன்

**கிளை A**  

**[SFX: ரிங்]**  
**Fake Officer:** வணக்கம், TANGEDCO ஹெல்ப்லைன். சார், பில் நிலுங்குது. முப்பது நிமிஷத்துல கனெக்ஷன் கட். வெரிஃபிகேஷனுக்கு இந்த லிங்குல “சப்போர்ட் ஆப்” டவுன்லோட் பண்ணுங்க.  
**Ramesh:** சரி சரி, பண்றேன்…  
**Narrator:** நில். அந்நியருக்கு ஃபோன் கட்டுப்பாடு கிடைக்கும். **பேட்டர்ன்:** உண்மையான அதிகாரி Anydesk மாதிரி ஸ்கிரீன் ஷேர் ஆப்பை அவசரப்படுத்த மாட்டார்.  
**Fake Officer:** எங்க சிஸ்டம்ல சின்க் ஆகல. இந்த லிங்குல **பத்து ரூபாய்** வெரிஃபிகேஷன் கட்டுங்க — கனெக்ஷன் பாதுகாப்பு.

**கிளை B**  

**Ramesh:** முதல்ல அதிகாரப்பூர்வ ஆப் பார்க்கிறேன்.  
**[SFX: டேப்]**  
**Ramesh:** நில்… “Paid”னு வருது. பாக்கி இல்ல. அப்புறம் இந்த மெசேஜ் ஏன்?  
**Narrator:** பயத்தோட போலி மெசேஜ்.  
**[SFX: இரண்டாவது SMS]**  
**Narrator:** புது எண்ணிலிருந்து அதே கதை.  
**Fake Officer:** சார், இந்த லிங்குல **பத்து ரூபாய்** — இல்லன்னா இன்று இரவு கட்.

**கிளை C**  

**Priya:** அப்பா, அனுப்புனவர் பாருங்க — பத்து இலக்க மொபைல்; AD-TANGEDCO மாதிரி குறுஞ்செய்தி இல்ல. வாக்கியம் — “your bil is block” மாதிரி தப்பு. இது ஸ்கேம்.  
**Narrator:** இரண்டு குறிப்புகள்: **யார்** அனுப்பினார், **எப்படி** எழுதினார்.  
**[SFX: SMS]**  
**Fake Officer:** கடைசி நோட்டீஸ் — இந்த லிங்குல **பத்து ரூபாய்**.

**— நோடு 2**  
A) பத்து ரூபாய் சின்னதுதான், கட்டிடுறேன் · B) நிறுத்து — SMS லிங்குல **UPI PIN** போட மாட்டேன் · C) உண்மையான ஹெல்ப்லைனுக்கு புகார்; எண்ணை ப்ளாக்

**— `ep01_outcome_pin_trap` (முழு VO)**  
**Narrator:** ரமேஷ் “பத்து ரூபாய்” லிங்கை அழுத்தி **UPI PIN** போட்டிருந்தா என்று கற்பனை பண்ணுங்க.  
**[SFX: மெல்லிய பதற்றம் — பயங்கரம் இல்லை]**  
**Narrator:** அந்த பக்கம் **போலி**. பத்து ரூபாய் TANGEDCO-க்குப் போகல — PIN பிடிக்க வடிவமைக்கப்பட்டது. கணக்கு விநாடிகளில் காலி; சிலர் முதல்ல சிறு தொகை வெட்டி, நீங்கள் சந்தேகப்படாம இருக்க வச்சிடுவாங்க.  
**Priya:** அப்பா!  
**Narrator:** சிமுலேட்டர்ல நீங்க **நோக்கமா** ஆபத்தான தேர்வு எடுத்தீங்க — **பேட்டர்ன்** புரிய. உண்மை வாழ்க்கையில்: ஒரு மூச்சு, **அதிகாரப்பூர்வ ஆப்** அல்லது **அசல் பில்லில்** இருக்க நம்பர் — ஒரு சரிபார்ப்பே போதும். **பேட்டர்ன்:** சிறு தொகை, பெரிய சேதம் — பத்து ரூபாய் விஷயமே இல்ல.

**— `ep01_outcome_safe` (முழு VO)**  
**Ramesh:** SMS லிங்குல என் **UPI PIN** போட மாட்டேன்.  
**[SFX: ஆப் மூடு / மூச்சு]**  
**Priya:** சரி. **அதிகாரப்பூர்வ ஆப்ல** ஏற்கனவே “Paid”னு இருக்கு.  
**Narrator:** “பத்து ரூபாய்தானே”னு சொல்றதுனால சின்ன விஷயமா தோணும். ஆபத்து தொகை இல்ல — **யார்** உங்க விரலைப் பார்க்கிறாங்க. உண்மையான கட்டணம்: நம்பும் **UPI ஆப்**, அல்லது **நீங்களே டைப்** பண்ணிய வலைத்தளம் — SMS-ல வந்த அந்நிய பக்கம் இல்ல.  
**Ramesh:** பங்கா ஓடுது. நாம சேப்பு.  
**Narrator:** டிஜிட்டல் ஞானம் = பயப்படாதீங்கனு அர்த்தமில்ல — **விரல் போகுமுன் நிற்கணும்**.

**— `ep01_outcome_report` (முழு VO)**  
**Priya:** இந்த மெசேஜ்க்கு ஸ்கிரீன்ஷாட் எடுக்கிறேன் — சைபர் ஹெல்ப்லைன் / போர்ட்டலுக்கு.  
**Ramesh:** இந்த எண்ணை **இப்பவே** ப்ளாக் பண்றேன்.  
**[SFX: ப்ளாக்]**  
**Narrator:** ப்ளாக் + புகார் — உங்கள மட்டும் இல்ல, **அடுத்தவங்களையும்** காக்கும். எண் மாத்துவாங்க, ஆனா புகார்கள் **சேரும்**.  
**Ramesh:** அடுத்த தடவ பதற்றத்துக்கு முன்னாடி **அதிகாரப்பூர்வ சேனல்** திறப்பேன்.  
**Narrator:** பழக்கம் இதுதான்: **முதல்ல நம்பகமான சேனல்** — அந்நிய லிங்க், ஒருபோதும் இல்ல.

**மிஷன்:** இன்று பாட்டி/தாத்தாவுடன் “லிங்க் கிளிக்” மெசேஜ் ஒன்றைத் திறந்து **அவசரம்**, **அனுப்புநர்**, **மொழி** — சொல்லி ஒண்ணா டிலீட் பண்ணுங்க.

---

### 5.4 Telugu (Te)

**Narrator:** రమేష్ గారు విశ్రాంతి తీసుకుంటున్నారు. కూతురు ప్రియా పక్క గదిలో ఆఫీసు పని. ఫోన్ మోగుతుంది.

**Ramesh:** ఏంటీ… రాత్రి ఎనిమిదికి ఎవరు మెసేజ్?

**[SFX: నోటిఫికేషన్]**  

**Ramesh:** ప్రియా! చూడు… ఈ రాత్రి కరెంట్ కట్ చేస్తామంటోంది, “లాస్ట్ బిల్ అప్‌డేట్ కాలేదు.” “బ్లాకౌట్ తప్పించుకో లింక్.”

**Priya:** నాన్న, పట్టించుకోకు… ఆగు, బిల్ కట్టామా?

**Ramesh:** కట్టామనుకుంట… మర్చిపోలేదు కదా? బయట నలభై డిగ్రీలు. ఫ్యాన్ ఆగితే నిద్రే రాదు. ఇక్కడ నంబర్ — “వెంటనే విద్యుత్ అధికారిని కాల్ చేయండి.”

**— నోడ్ 1**  
A) SMS లోని నంబర్‌కు వెంటనే కాల్ · B) డిస్కాం **అధికారిక** యాప్ లేదా వెబ్‌సైట్ తెరిచి చెక్ · C) ప్రియాతో కలిసి SMS జాగ్రత్తగా చదవించు

**శాఖ A**  

**[SFX: రింగ్]**  
**Fake Officer:** నమస్కారం, విద్యుత్ హెల్ప్‌లైన్. సార్, బిల్ పెండింగ్. ముప్పై నిమిషాల్లో కనెక్షన్ కట్. ధృవీకరణకు ఈ లింక్ నుంచి “సపోర్ట్ యాప్” డౌన్‌లోడ్ చేయండి.  
**Ramesh:** అవును అవును, చేస్తాను…  
**Narrator:** ఆగండి. అపరిచితుడికి ఫోన్ నియంత్రణ ఇచ్చినట్లే. **ప్యాటర్న్:** నిజమైన అధికారి స్క్రీన్-షేర్ యాప్‌లకు తొందరపెట్టరు.  
**Fake Officer:** మా సిస్టమ్‌లో సింక్ కాలేదు. ఈ లింక్ ద్వారా **పది రూపాయలు** వెరిఫికేషన్ ఫీజు — కనెక్షన్ సురక్షితం.

**శాఖ B**  

**Ramesh:** మొదట అధికారిక యాప్ చూస్తాను.  
**[SFX: ట్యాప్]**  
**Ramesh:** ఆగు… “Paid.” బకాయి లేదు. అయితే ఈ మెసేజ్ ఎందుకు?  
**Narrator:** భయపెట్టే ఫాంటమ్ మెసేజ్.  
**[SFX: రెండవ SMS]**  
**Narrator:** కొత్త నంబర్ — అదే మాట.  
**Fake Officer:** సార్, ఈ లింక్‌లో **పది రూపాయలు** — లేకుంటే ఈ రాత్రి కట్.

**శాఖ C**  

**Priya:** నాన్న, పంపినవారు చూడు — పది అంకెల మొబైల్; అధికారిక షార్ట్ కోడ్ లాంటిది కాదు. వాక్యాలు — “your account is block” లాంటి తప్పులు. ఇది స్కామ్.  
**Narrator:** రెండు సూచనలు: **ఎవరు** పంపారు, **ఎలా** రాశారు.  
**[SFX: SMS]**  
**Fake Officer:** చివరి నోటీసు — ఈ లింక్‌లో **పది రూపాయలు**.

**— నోడ్ 2**  
A) పది రూపాయలు చిన్నదే, కట్టేస్తా · B) ఆగు — SMS లింక్‌లో **UPI PIN** ఇవ్వను · C) నిజమైన హెల్ప్‌లైన్‌కు రిపోర్ట్, నంబర్ బ్లాక్

**— `ep01_outcome_pin_trap` (పూర్తి VO)**  
**Narrator:** రమేష్ “పది రూపాయలు” లింక్ నొక్కి **UPI PIN** ఇచ్చి ఉంటే అనుకోండి.  
**[SFX: మృదువైన ఉద్రిక్తత — భయపెట్టదు]**  
**Narrator:** ఆ పేజీ **నకిలీ**. పది రూపాయలు డిస్కాంకు వెళ్లడం కాదు — PIN పట్టుకోవడానికి. ఖాతా క్షణాల్లో ఖాళీ; మొదట చిన్న మొత్తం తీసి మీరు గమనించకుండా ఉంచే పద్ధతి కూడా ఉంటుంది.  
**Priya:** నాన్న!  
**Narrator:** సిమ్యులేటర్‌లో మీరు **సావధానంగా** రిస్కీ ఎంపిక చేశారు — **ప్యాటర్న్** చూడటానికి. నిజ జీవితంలో: ఒక ఊపిరి, **అధికారిక యాప్** లేదా **బిల్లుపై** ఉన్న నంబర్ — ఒక్క ధృవీకరణ చాలు. **ప్యాటర్న్:** చిన్న మొత్తం, పెద్ద నష్టం — పది రూపాయల విషయం కాదు.

**— `ep01_outcome_safe` (పూర్తి VO)**  
**Ramesh:** SMS లింక్‌లో నా **UPI PIN** ఇవ్వను.  
**[SFX: యాప్ మూసి / ఊపిరి]**  
**Priya:** సరే. **అధికారిక యాప్**‌లో ఇప్పటికే “Paid.”  
**Narrator:** “పది రూపాయలే కదా” అనేది చిన్నదిగా అనిపిస్తుంది. ప్రమాదం మొత్తం కాదు — **ఎవరు** మీ వేలును చూస్తున్నారో. నిజమైన చెల్లింపు: మీరు నమ్మే **UPI యాప్**, లేదా **మీరే టైప్** చేసిన సైట్ — SMS నుంచి తెరిచిన అపరిచిత పేజీ కాదు.  
**Ramesh:** ఫ్యాన్ నడుస్తోంది. మనం సేఫ్.  
**Narrator:** డిజిటల్ జ్ఞానం అంటే భయం లేకుండా ఉండటం కాదు — **వేలు పోయే ముందు ఆగడం**.

**— `ep01_outcome_report` (పూర్తి VO)**  
**Priya:** ఈ మెసేజ్ స్క్రీన్‌షాట్ తీస్తాను — సైబర్ హెల్ప్‌లైన్ / పోర్టల్ కోసం.  
**Ramesh:** ఈ నంబర్‌ను **ఇప్పుడే** బ్లాక్ చేస్తాను.  
**[SFX: బ్లాక్]**  
**Narrator:** బ్లాక్ + రిపోర్ట్ — మీరు మాత్రమే కాదు, **తర్వాత వారినీ** కాపాడతారు. నంబర్లు మారుస్తారు, కానీ ఫిర్యాదులు **పేరుకుపోతాయి**.  
**Ramesh:** తర్వాత సారి భయపడే ముందు **అధికారిక చానెల్** తెరుస్తాను.  
**Narrator:** అలవాటు ఇదే: **మొదట నమ్మకమైన చానెల్** — అపరిచిత లింక్, ఎప్పుడూ వద్దు.

**మిషన్:** ఈరోజు తాతయ్య/నానమ్మతో “లింక్ క్లిక్” మెసేజ్ ఒకటి తెరవండి. **తొందర**, **పంపినవారు**, **భాష** — చెప్పి కలిసి డిలీట్ చేయండి.

---

### 5.5 Kannada (Kn)

**Narrator:** ರಮೇಶ್ ಅವರು ವಿಶ್ರಾಂತಿ ತೆಗೆದುಕೊಳ್ಳುತ್ತಿದ್ದಾರೆ. ಪ್ರಿಯಾ ಮುಂದಿನ ಕೋಣೆಯಲ್ಲಿ ಕಚೇರಿ ಕೆಲಸ. ಫೋನ್ ಬಾರಿಸುತ್ತದೆ.

**Ramesh:** ಅಯ್ಯೋ… ರಾತ್ರಿ ಎಂಟಕ್ಕೆ ಯಾರು ಮೆಸೇಜ್?

**[SFX: ನೋಟಿಫಿಕೇಶನ್]**  

**Ramesh:** ಪ್ರಿಯಾ! ನೋಡು… ಇಂದು ರಾತ್ರಿ ವಿದ್ಯುತ್ ಕಡಿತವಂತೆ, “ಲಾಸ್ಟ್ ಬಿಲ್ ಅಪ್‌ಡೇಟ್ ಆಗಿಲ್ಲ.” “ಬ್ಲ್ಯಾಕ್‌ಔಟ್ ತಪ್ಪಿಸಿ ಲಿಂಕ್.”

**Priya:** ಅಪ್ಪಾ, ಬಿಡು… ನಿಲ್ಲು, ಬಿಲ್ ಪಾವತಿ ಮಾಡಿದ್ವಾ?

**Ramesh:** ಮಾಡಿದ್ದೀನಿ ಅನ್ಕೊಳ್ತಾ ಇದ್ದೀನಿ… ಮರ್ತಿದೀನಾ? ಹೊರಗೆ ನಲವತ್ತು ಡಿಗ್ರಿ. ಫ್ಯಾನ್ ನಿಂತರೆ ನಿದ್ದೆ ಬರಲ್ಲ. ಇಲ್ಲಿ ನಂಬರ್ — “ತಕ್ಷಣ BESCOM ಅಧಿಕಾರಿಯನ್ನು ಕರೆ ಮಾಡಿ.”

**— ನೋಡ್ 1**  
A) SMS ನಲ್ಲಿನ ನಂಬರ್‌ಗೆ ತಕ್ಷಣ ಕರೆ · B) BESCOM / **ಅಧಿಕೃತ** ವಿದ್ಯುತ್ ಯಾಪ್ ಅಥವಾ ವೆಬ್‌ಸೈಟ್ ತೆರೆದು ಪರಿಶೀಲಿಸು · C) ಪ್ರಿಯಾಳ ಜೊತೆ SMS ಒಟ್ಟಿಗೆ ಓದಿ ನೋಡು

**ಶಾಖೆ A**  

**[SFX: ರಿಂಗ್]**  
**Fake Officer:** ನಮಸ್ಕಾರ, ವಿದ್ಯುತ್ ಹೆಲ್ಪ್‌ಲೈನ್. ಸರ್, ಬಿಲ್ ಬಾಕಿ. ಮೂವತ್ತು ನಿಮಿಷದಲ್ಲಿ ಕನೆಕ್ಷನ್ ಕಟ್. ಪರಿಶೀಲನೆಗೆ ಈ ಲಿಂಕ್‌ನಿಂದ “ಸಪೋರ್ಟ್ ಯಾಪ್” ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ.  
**Ramesh:** ಹೌದು ಹೌದು, ಮಾಡ್ತೀನಿ…  
**Narrator:** ನಿಲ್ಲಿ. ಅಪರಿಚಿತರಿಗೆ ಫೋನ್ ನಿಯಂತ್ರಣ. **ಪ್ಯಾಟರ್ನ್:** ನಿಜವಾದ ಅಧಿಕಾರಿ ಸ್ಕ್ರೀನ್-ಶೇರ್ ಯಾಪ್‌ಗೆ ತೊಂದರೆ ಮಾಡುವುದಿಲ್ಲ.  
**Fake Officer:** ನಮ್ಮ ಸಿಸ್ಟಮ್‌ನಲ್ಲಿ ಸಿಂಕ್ ಆಗಿಲ್ಲ. ಈ ಲಿಂಕ್‌ನಲ್ಲಿ **ಹತ್ತು ರೂಪಾಯಿ** ಪರಿಶೀಲನಾ ಶುಲ್ಕ — ಕನೆಕ್ಷನ್ ಸುರಕ್ಷಿತ.

**ಶಾಖೆ B**  

**Ramesh:** ಮೊದಲು ಅಧಿಕೃತ ಯಾಪ್ ನೋಡ್ತೀನಿ.  
**[SFX: ಟ್ಯಾಪ್]**  
**Ramesh:** ನಿಲ್ಲು… “Paid.” ಬಾಕಿ ಇಲ್ಲ. ಆ ಮೆಸೇಜ್ ಏಕೆ?  
**Narrator:** ಭಯದ ಫ್ಯಾಂಟಮ್ ಮೆಸೇಜ್.  
**[SFX: ಎರಡನೇ SMS]**  
**Narrator:** ಹೊಸ ಸಂಖ್ಯೆ — ಅದೇ ಕಥೆ.  
**Fake Officer:** ಸರ್, ಈ ಲಿಂಕ್‌ನಲ್ಲಿ **ಹತ್ತು ರೂಪಾಯಿ** — ಇಲ್ಲದಿದ್ದರೆ ಈ ರಾತ್ರಿ ಕಟ್.

**ಶಾಖೆ C**  

**Priya:** ಅಪ್ಪಾ, ಕಳುಹಿಸಿದವರನ್ನು ನೋಡಿ — ಹತ್ತು ಅಂಕಿಯ ಮೊಬೈಲ್; ಅಧಿಕೃತ ಶಾರ್ಟ್ ಕೋಡ್ ಅಲ್ಲ. ವಾಕ್ಯಗಳು — “your account is block” ಎಂಬ ತಪ್ಪುಗಳು. ಇದು ಸ್ಕ್ಯಾಮ್.  
**Narrator:** ಎರಡು ಸೂಚನೆಗಳು: **ಯಾರು** ಕಳುಹಿಸಿದರು, **ಹೇಗೆ** ಬರೆದರು.  
**[SFX: SMS]**  
**Fake Officer:** ಅಂತಿಮ ನೋಟೀಸ್ — ಈ ಲಿಂಕ್‌ನಲ್ಲಿ **ಹತ್ತು ರೂಪಾಯಿ**.

**— ನೋಡ್ 2**  
A) ಹತ್ತು ರೂಪಾಯಿ ಚಿಕ್ಕದು, ಪಾವತಿಸುತ್ತೇನೆ · B) ನಿಲ್ಲು — SMS ಲಿಂಕ್‌ನಲ್ಲಿ **UPI PIN** ಇಡುವುದಿಲ್ಲ · C) ನಿಜವಾದ ಹೆಲ್ಪ್‌ಲೈನ್‌ಗೆ ದೂರು, ನಂಬರ್ ಬ್ಲಾಕ್

**— `ep01_outcome_pin_trap` (ಪೂರ್ಣ VO)**  
**Narrator:** ರಮೇಷ್ “ಹತ್ತು ರೂಪಾಯಿ” ಲಿಂಕ್ ಒತ್ತಿ **UPI PIN** ಹಾಕಿದ್ದರೆ ಎಂದು ಊಹಿಸಿ.  
**[SFX: ಮೃದು ಒತ್ತಡ — ಭಯ ಹುಟ್ಟಿಸದು]**  
**Narrator:** ಆ ಪುಟ **ನಕಲಿ**. ಹತ್ತು ರೂಪಾಯಿ ಬೋರ್ಡ್‌ಗೆ ಹೋಗುತ್ತಿಲ್ಲ — PIN ಸೆರೆಹಿಡಿಯಲು. ಖಾತೆ ಸೆಕೆಂಡುಗಳಲ್ಲಿ ಖಾಲಿ; ಮೊದಲು ಚಿಕ್ಕ ಮೊತ್ತ ಕಡಿತ ಮಾಡಿ ನೀವು ಗಮನಿಸದಂತೆ ಇರಿಸಬಹುದು.  
**Priya:** ಅಪ್ಪಾ!  
**Narrator:** ಸಿಮ್ಯುಲೇಟರ್‌ನಲ್ಲಿ ನೀವು **ಉದ್ದೇಶಪೂರ್ವಕ** ಅಪಾಯಕಾರಿ ಆಯ್ಕೆ ಮಾಡಿದ್ದೀರಿ — **ನಮೂನೆ** ಕಾಣಲು. ನಿಜ ಜೀವನದಲ್ಲಿ: ಒಂದು ಉಸಿರು, **ಅಧಿಕೃತ ಯಾಪ್** ಅಥವಾ **ನಿಜವಾದ ಬಿಲ್** ಮೇಲಿನ ಸಂಖ್ಯೆ — ಒಂದು ಪರಿಶೀಲನೆ ಸಾಕು. **ನಮೂನೆ:** ಚಿಕ್ಕ ಮೊತ್ತ, ದೊಡ್ಡ ನಷ್ಟ — ಹತ್ತು ರೂಪಾಯಿ ಪ್ರಶ್ನೆಯೇ ಅಲ್ಲ.

**— `ep01_outcome_safe` (ಪೂರ್ಣ VO)**  
**Ramesh:** SMS ಲಿಂಕ್‌ನಲ್ಲಿ ನನ್ನ **UPI PIN** ಇಡುವುದಿಲ್ಲ.  
**[SFX: ಯಾಪ್ ಮುಚ್ಚು / ಉಸಿರು]**  
**Priya:** ಸರಿ. **ಅಧಿಕೃತ ಯಾಪ್**‌ನಲ್ಲಿ ಈಗಾಗಲೇ “Paid.”  
**Narrator:** “ಹತ್ತು ರೂಪಾಯಿ ಮಾತ್ರ” ಎಂದು ಚಿಕ್ಕದಾಗಿ ಕಾಣುತ್ತದೆ. ಅಪಾಯ ಮೊತ್ತವಲ್ಲ — **ಯಾರು** ನಿಮ್ಮ ಬೆರಳುಗಳನ್ನು ನೋಡುತ್ತಿದ್ದಾರೆ. ನಿಜವಾದ ಪಾವತಿ: ನೀವು ನಂಬುವ **UPI ಯಾಪ್**, ಅಥವಾ **ನೀವೇ ಟೈಪ್** ಮಾಡಿದ ಸೈಟ್ — SMS ನಿಂದ ತೆರೆದ ಅಪರಿಚಿತ ಪುಟವಲ್ಲ.  
**Ramesh:** ಫ್ಯಾನ್ ಓಡುತ್ತಿದೆ. ನಾವು ಸುರಕ್ಷಿತ.  
**Narrator:** ಡಿಜಿಟಲ್ ಜ್ಞಾನ ಅಂದರೆ ಭಯವೇ ಇರಬಾರದು ಎಂದಲ್ಲ — **ಬೆರಳು ಹೋಗುವ ಮೊದಲು ನಿಲ್ಲುವುದು**.

**— `ep01_outcome_report` (ಪೂರ್ಣ VO)**  
**Priya:** ಈ ಮೆಸೇಜ್‌ನ ಸ್ಕ್ರೀನ್‌ಶಾಟ್ ತೆಗೆಯುತ್ತೇನೆ — ಸೈಬರ್ ಹೆಲ್ಪ್‌ಲೈನ್ / ಪೋರ್ಟಲ್‌ಗೆ.  
**Ramesh:** ಈ ಸಂಖ್ಯೆಯನ್ನು **ಈಗಲೇ** ಬ್ಲಾಕ್ ಮಾಡುತ್ತೇನೆ.  
**[SFX: ಬ್ಲಾಕ್]**  
**Narrator:** ಬ್ಲಾಕ್ + ದೂರು — ನಿಮ್ಮನ್ನು ಮಾತ್ರವಲ್ಲ, **ಮುಂದಿನವರನ್ನು** ಕಾಪಾಡುತ್ತದೆ. ಸಂಖ್ಯೆಗಳು ಬದಲಾಗುತ್ತವೆ, ಆದರೆ ದೂರುಗಳು **ಸೇರುತ್ತವೆ**.  
**Ramesh:** ಮುಂದೆ ಭಯಪಡುವ ಮೊದಲು **ಅಧಿಕೃತ ಚಾನೆಲ್** ತೆರೆಯುತ್ತೇನೆ.  
**Narrator:** ಅಭ್ಯಾಸ ಇದು: **ಮೊದಲು ನಂಬಲರ್ಹ ಚಾನೆಲ್** — ಅಪರಿಚಿತ ಲಿಂಕ್, ಎಂದಿಗೂ ಇಲ್ಲ.

**ಮಿಷನ್:** ಇಂದು ಅಜ್ಜಿ/ಅಜ್ಜನೊಂದಿಗೆ “ಲಿಂಕ್ ಕ್ಲಿಕ್” ಮೆಸೇಜ್ ತೆರೆಯಿರಿ. **ತ್ವರೆ**, **ಕಳುಹಿಸಿದವರು**, **ಭಾಷೆ** — ಹೇಳಿ ಒಟ್ಟಿಗೆ ಡಿಲೀಟ್ ಮಾಡಿ.

---

### 5.6 Malayalam (Ml) — KSEB

**Narrator:** രമേശ് സാർ വിശ്രമിക്കുന്നു. മകൾ പ്രിയ അടുത്ത മുറിയിൽ ഓഫീസ് ജോലി. ഫോൺ മുഴങ്ങുന്നു.

**Ramesh:** ഹേ… രാത്രി എട്ടുമണിക്ക് ആരാണ് മെസേജ്?

**[SFX: നോട്ടിഫിക്കേഷൻ]**  

**Ramesh:** പ്രിയാ! നോക്ക്… ഇന്ന് രാത്രി വൈദ്യുതി മുറിക്കുമെന്ന്, “ലാസ്റ്റ് ബിൽ അപ്ഡേറ്റ് ചെയ്തില്ല.” “ബ്ലാക്കൗട്ട് ഒഴിവാക്കാൻ ലിങ്ക്.”

**Priya:** അച്ഛാ, അവഗണിക്കു… നിൽക്ക്, ബിൽ അടച്ചില്ലേ?

**Ramesh:** അടച്ചുവെന്ന് തോന്നുന്നു… മറന്നോ? പുറത്ത് നാല്പത് ഡിഗ്രി. ഫാൻ നിന്നാൽ ഉറക്കം വരില്ല. ഇവിടെ നമ്പർ — “ഉടൻ KSEB ഓഫീസറെ വിളിക്കുക.”

**— നോഡ് 1**  
A) SMS-ലെ നമ്പറിലേക്ക് ഉടൻ വിളിക്കും · B) KSEB **ഔദ്യോഗിക** ആപ്പ് അല്ലെങ്കിൽ വെബ്സൈറ്റ് തുറന്ന് പരിശോധിക്കും · C) പ്രിയയോടൊപ്പം SMS ഒന്നിച്ച് ശ്രദ്ധിച്ച് വായിക്കും

**ശാഖ A**  

**[SFX: റിംഗ്]**  
**Fake Officer:** നമസ്കാരം, KSEB ഹെൽപ്‌ലൈൻ. സർ, ബിൽ ബാക്കി. മുപ്പത് മിനിറ്റിനുള്ളിൽ കണക്ഷൻ കട്ട്. വെരിഫിക്കേഷനായി ഈ ലിങ്കിൽ നിന്ന് “സപ്പോർട്ട് ആപ്പ്” ഡൗൺലോഡ് ചെയ്യുക.  
**Ramesh:** ശരി ശരി, ചെയ്യുന്നു…  
**Narrator:** നിർത്തുക. അപരിചിതന് ഫോൺ നിയന്ത്രണം. **പാറ്റേൺ:** യഥാർത്ഥ ഉദ്യോഗസ്ഥൻ സ്ക്രീൻ-ഷെയർ ആപ്പിന് തിരക്ക് കാണിക്കില്ല.  
**Fake Officer:** ഞങ്ങളുടെ സിസ്റ്റത്തിൽ സിങ്ക് ആയില്ല. ഈ ലിങ്കിൽ **പത്തു രൂപ** വെരിഫിക്കേഷൻ ഫീസ് — കണക്ഷൻ സുരക്ഷിതം.

**ശാഖ B**  

**Ramesh:** ആദ്യം ഔദ്യോഗിക ആപ്പ് നോക്കട്ടെ.  
**[SFX: ടാപ്]**  
**Ramesh:** നിൽക്ക്… “Paid.” ബാക്കി ഇല്ല. എങ്കിൽ ഈ മെസേജ് എന്തിന്?  
**Narrator:** ഭയത്തിന്റെ ഫാന്റം മെസേജ്.  
**[SFX: രണ്ടാമത്തെ SMS]**  
**Narrator:** പുതിയ നമ്പർ — അതേ കഥ.  
**Fake Officer:** സർ, ഈ ലിങ്കിൽ **പത്തു രൂപ** — ഇല്ലെങ്കിൽ ഇന്ന് രാത്രി കട്ട്.

**ശാഖ C**  

**Priya:** അച്ഛാ, അയച്ചവരെ നോക്ക് — പത്തക്ക നമ്പർ മൊബൈൽ; ഔദ്യോഗിക ഷോർട്ട് കോഡ് പോലെയല്ല. വാക്യങ്ങൾ — “your account is block” പോലുള്ള പിശകുകൾ. ഇത് സ്കാം.  
**Narrator:** രണ്ട് സൂചനകൾ: **ആര്** അയച്ചു, **എങ്ങനെ** എഴുതി.  
**[SFX: SMS]**  
**Fake Officer:** അന്തിമ നോട്ടീസ് — ഈ ലിങ്കിൽ **പത്തു രൂപ**.

**— നോഡ് 2**  
A) പത്തു രൂപ ചെറിയതാണ്, അടയ്ക്കാം · B) നിർത്തുക — SMS ലിങ്കിൽ **UPI PIN** ഇടില്ല · C) യഥാർത്ഥ ഹെൽപ്‌ലൈനിൽ റിപ്പോർട്ട്, നമ്പർ ബ്ലോക്ക്

**— `ep01_outcome_pin_trap` (പൂർണ്ണ VO)**  
**Narrator:** രമേശ് “പത്തു രൂപ” ലിങ്കിൽ ടാപ്പ് ചെയ്ത് **UPI PIN** നൽകിയിരുന്നെങ്കിൽ എന്ന് കരുതുക.  
**[SFX: മൃദുലമായ ഉദ്വേഗം — ഭയം ഉണർത്താതെ]**  
**Narrator:** ആ പേജ് **നകലി** ആയിരുന്നു. പത്തു രൂപ KSEB-യിലേക്ക് പോകുന്നില്ല — PIN പിടിച്ചെടുക്കാൻ. അക്കൗണ്ട് സെക്കൻഡുകളിൽ ശൂന്യമാകാം; ചിലർ ആദ്യം ചെറിയ തുക വെട്ടി നിങ്ങൾക്ക് സംശയം തോന്നാതെ ഇരിക്കും.  
**Priya:** അച്ഛാ!  
**Narrator:** സിമുലേറ്ററിൽ നിങ്ങൾ **ഉദ്ദേശപൂർവം** അപകടകരമായ വഴി തിരഞ്ഞെടുത്തു — **പാറ്റേൺ** കാണാൻ. യഥാർത്ഥ ജീവിതത്തിൽ: ഒരു ശ്വാസം, **ഔദ്യോഗിക ആപ്പ്** അല്ലെങ്കിൽ **യഥാർത്ഥ ബില്ലിലെ** നമ്പർ — ഒരു പരിശോധന മതി. **പാറ്റേൺ:** ചെറിയ തുക, വലിയ നഷ്ടം — പത്തു രൂപയുടെ കാര്യമല്ല.

**— `ep01_outcome_safe` (പൂർണ്ണ VO)**  
**Ramesh:** SMS ലിങ്കിൽ എന്റെ **UPI PIN** ഇടില്ല.  
**[SFX: ആപ്പ് അടയ്ക്കൽ / ശ്വാസം]**  
**Priya:** ശരി. **ഔദ്യോഗിക ആപ്പിൽ** ഇതിനകം “Paid.”  
**Narrator:** “പത്തു രൂപ മാത്രം” എന്ന് ചെറുതായി തോന്നും. അപകടം തുകയല്ല — **ആര്** നിങ്ങളുടെ വിരലുകൾ നോക്കുന്നു. യഥാർത്ഥ പേയ്‌മെന്റ്: നിങ്ങൾ വിശ്വസിക്കുന്ന **UPI ആപ്പ്**, അല്ലെങ്കിൽ **നിങ്ങൾ തന്നെ ടൈപ്പ്** ചെയ്ത വെബ്സൈറ്റ് — SMS-ൽ നിന്ന് തുറന്ന അപരിചിത പേജ് അല്ല.  
**Ramesh:** ഫാൻ ഇപ്പോഴും ഓടുന്നു. നമ്മൾ സുരക്ഷിതം.  
**Narrator:** ഡിജിറ്റൽ ജ്ഞാനം എന്നാൽ ഭയം ഇല്ലാതിരിക്കുക എന്നല്ല — **വിരൽ പോകുന്നതിന് മുമ്പ് നിർത്തുക**.

**— `ep01_outcome_report` (പൂർണ്ണ VO)**  
**Priya:** ഈ മെസേജിന്റെ സ്ക്രീൻഷോട്ട് എടുക്കാം — സൈബർ ഹെൽപ്‌ലൈൻ / പോർട്ടലിന്.  
**Ramesh:** ഈ നമ്പർ **ഇപ്പോൾ തന്നെ** ബ്ലോക്ക് ചെയ്യുന്നു.  
**[SFX: ബ്ലോക്ക്]**  
**Narrator:** ബ്ലോക്കും റിപ്പോർട്ടും — നിങ്ങളെ മാത്രമല്ല, **അടുത്ത ആളെയും** സംരക്ഷിക്കുന്നു. നമ്പറുകൾ മാറ്റും, പക്ഷേ പരാതികൾ **കൂടിക്കൊണ്ടിരിക്കും**.  
**Ramesh:** അടുത്ത തവണ ഭയക്കുന്നതിന് മുമ്പ് **യഥാർത്ഥ ചാനൽ** തുറക്കും.  
**Narrator:** ശീലം ഇതാണ്: **ആദ്യം വിശ്വസനീയമായ ചാനൽ** — അപരിചിത ലിങ്ക്, ഒരിക്കലും വേണ്ട.

**മിഷൻ:** ഇന്ന് മുത്തശ്ശി/മുത്തച്ഛനൊപ്പം “ലിങ്ക് ക്ലിക്ക്” മെസേജ് തുറക്കുക. **അത്യാവശ്യം**, **അയച്ചവർ**, **ഭാഷ** — പറഞ്ഞ് ഒന്നിച്ച് ഡിലീറ്റ് ചെയ്യുക.

---

### 5.7 L10n QA checklist (VO + UI)

| Check | Detail |
|-------|--------|
| **Board name** | Match state DISCOM your audience expects (Ta: TANGEDCO; Kl: KSEB; Kn: BESCOM or statewide copy; Te/Hi: generic + “official app”). |
| **Node 2 bridge** | Branches B & C must include **second message** VO before the ₹10 beat (see §1 continuity). |
| **Scammer tone** | Respectful, efficient “helpline” — not melodrama villain. |
| **Terminal MP3s** | §5.1–5.6 include full VO for `ep01_outcome_pin_trap`, `ep01_outcome_safe`, `ep01_outcome_report` — three files per language (same filenames on CDN, different story rows or asset folders per locale). |
| **UI strings** | Localize `interactive_graph` `choices[].label` to match §5.1–5.6 node labels for that locale build (or one story row per language). |
| **PIN / UPI** | Keep **UPI PIN** / **UPI** as loanwords where natural; explain once per episode for seniors. |
