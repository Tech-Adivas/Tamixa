# Tamixa EduStory — content matrix and rollout roadmap

**Purpose:** **Pillar × age band** flagship series (working titles), **six-month content spine**, and **phased product/engineering rollout**. Subscription pricing is **out of scope** here; channels (B2C parent, school, CSR) are noted for later packaging.  
**Companion docs:** [EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md), [EDUSTORY_VALUES_AND_PRINCIPLES.md](EDUSTORY_VALUES_AND_PRINCIPLES.md), [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md), [EDUCATIONAL_FEATURES_INDEX.md](EDUCATIONAL_FEATURES_INDEX.md). **Simulator authoring & taxonomy:** [admin/EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md](admin/EDUSTORY_SIMULATOR_CONTENT_BLUEPRINT.md), [admin/EDU_METADATA_CONVENTIONS.md](admin/EDU_METADATA_CONVENTIONS.md).

---

## 1. Pillar priority

| Order | Pillar | Focus |
|-------|--------|--------|
| **A (launch 1)** | Digital life and judgment | Forwards, DMs, deepfakes, workplace phishing, family group panic, elder scams |
| **B (launch 2)** | Money in Indian life | Pocket money, UPI mistakes, influencer spend, salary week one, elder “emergency” link |
| **C (launch 3)** | Exam pressure, sleep, help-seeking | Teen stress; **safeguards** required ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §8–§10) |
| **D (launch 4)** | Work, research, detective thinking | Jobs, interviews, small business, source checking |

---

## 2. Pillar A — Digital life and judgment

| Age band | Flagship series (working title) | Target outcome |
|----------|----------------------------------|----------------|
| Kids 3–8 | **Forward Mat?** | Pause before forward; name one trusted adult to check with. |
| Kids 6–10 | **Stranger Voice** | Recognize “too private / too fast” patterns; tell a parent. |
| Kids 8–12 | **Deepfake Drama Club** | Looks real ≠ true; kindness; involve adult/teacher. |
| Teens 13–18 | **DM After Midnight** | Boundaries, sleep, reporting (conceptual). |
| Teens 15–18 | **Coaching Class Rumors** | Integrity under “shortcut” pressure. |
| Young adults 18–35 | **Office Forward** | Verify channel; no OTP; escalate mindset. |
| Parents 30–55 | **The Family Group** | Calm panic forwards; family rule on OTP. |
| Elders 55+ | **Digital Arrest** (dramatized, non-terror) | Urgency + secrecy pattern; hang up; verify; use official reporting (UI signpost only). |

---

## 3. Pillar B — Money in Indian life

| Age band | Flagship series (working title) | Target outcome |
|----------|----------------------------------|----------------|
| Kids 5–9 | **Pocket Money Monday** | Save / spend / give; talk to parent before lending. |
| Kids 8–12 | **Cricket Bet?** | Gambling pressure; refuse without losing face. |
| Teens 13–18 | **Influencer Spend** | Delayed gratification; family conversation opener. |
| Teens 15–18 | **First UPI Mistake** | Don’t panic-pay more; screenshot; get help (narrative, not legal advice). |
| Young adults 18–35 | **Salary Week One** | Order of payments; boundaries on asks. |
| Young adults | **Side Gig Trap** | Pay-to-work and urgency red flags. |
| Parents | **Tuition vs Trip** | One honest family money conversation via story. |
| Elders | **Beta Sent a Link** | Never share OTP; verify second channel; family code word. |

---

## 4. Pillar C — Exam pressure, sleep, help-seeking

| Age band | Flagship series (working title) | Target outcome |
|----------|----------------------------------|----------------|
| Teens 15–18 | **Three Hours Left** | Normalize struggle; signpost trusted adult / counselor in UI. |
| Teens | **Plan B Isn’t Failure** | Reduce catastrophizing; dignity in alternate paths. |
| Parents | **Marks Group Chat** | Support child without owning their marks; protect sleep. |

**Safeguard:** No implication that audio replaces therapy. Crisis resources in app shell where required by policy.

---

## 5. Pillar D — Work, research, thinking

| Age band | Flagship series (working title) | Target outcome |
|----------|----------------------------------|----------------|
| Teens / young adults | **Source or Story?** | Slower share; one verification habit. |
| Young adults | **First Client** | Boundaries; written agreement habit (concept). |
| Young adults | **Interview Ghost** | Verify company; never pay to get a job. |

---

## 6. Six-month content spine (minimum viable Edu credibility)

**Go-to-market hook (“Modern survival”):** Align months 1–4 with [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.3—**digital judgment + money taboo** first (painkillers vs passive “EdTapes”), then widen.

| Month | Content focus |
|-------|----------------|
| **1–2** | Pillar A kids: **Forward Mat?**, **Stranger Voice** (Hindi + one additional Indian language). |
| **2–3** | Pillar A elders + parents: **Digital Arrest** shorts, **The Family Group** (shared scam DNA, different tone). |
| **3–4** | Pillar B: **First UPI Mistake** (teens), **Pocket Money Monday** (kids). |
| **4–5** | Pillar A teens: **DM After Midnight**. |
| **5–6** | Start one Pillar C **pilot** with full safeguards review, or begin Pillar D **Source or Story?** shorts. |

Parallel: maintain **Fun** library cadence unchanged.

---

## 7. Cross-cutting product features (support all pillars)

| Feature | Notes |
|---------|--------|
| **Edu lane + metadata** | Fun vs Edu, pillar, age band, language—see Phase 1 below. |
| **Chapter marks in player** | Replay one scene; classroom or parent discussion. **Open design:** timed markers in CMS, sidecar JSON, or narration pipeline—see [backend/NARRATION_ARCHITECTURE.md](backend/NARRATION_ARCHITECTURE.md). |
| **Reflection pause in audio** | Produced in mastering or signaled by chapter type; product may show optional on-screen hint. |
| **Offline pack per series** | [mobile/OFFLINE_CACHE.md](mobile/OFFLINE_CACHE.md) when implementing. |
| **Co-listening / senior mode** | Speed, typography, simplified navigation. |
| **Discussion cards (PDF)** | Downloadable prompts for dinner-table / classroom use; pairs with facilitator packs ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.3). |
| **Transparency / support posture** | In-app copy consistent with operations: e.g. no outbound sales calls, help on user initiative ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.2). |
| **“Trial of truth” / pricing transparency** | Clear free vs paid and renewal/cancel paths—only what legal/product actually offer ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.5). |
| **Story-interrupt (Edu-only, optional)** | Reflection pauses; optional chapter gate with skip for accessibility ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.6). |
| **Scam-simulator + contradiction pairs** | Protagonist-in-scam audio; paired outcomes ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.5). |
| **“Curator’s challenge” shorts** | Daily or serial spot-the-pattern clips; regional languages prioritized ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.6–§9.8). |
| **Persistent helpline entry** | Visible help/helpline affordance on mental-health-adjacent Edu playback; numbers verified pre-launch ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.8, §10). |
| **Static helplines and disclaimers** | Outside fiction audio where appropriate ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §10). |
| **Optional quizzes / streaks** | After listening; [EDUCATIONAL_FEATURES_INDEX.md](EDUCATIONAL_FEATURES_INDEX.md)—secondary to narrative. |
| **Choice engine / pivot points** | Branching dilemmas on a timer; CMS tags for options and next chapter ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.1). |
| **Life-skill dashboard (guarded)** | Theme-based parent summaries; avoid faux-precision scores without validation ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.2). |
| **Dual POV (“two-sided”) stories** | Teen + parent threads; compromise unlock with solo-listen fallback ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.3). |
| **In-story sandbox tools** | Micro calculator / pitch template at chapter break; Edu only ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.4). |
| **CMS branch metadata** | Chaptering, decision prompts, skill tags—logic layer on current core ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.6). |
| **Family mission cards** | Post-episode real-world try; offline-first value ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.1). |
| **Decision journal (printable)** | Tangible choice log for parents/schools ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.1). |
| **Hyper-local dialect / metaphor** | Hinglish, Tanglish, city/region metaphor banks; editorial + AI guardrails ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.2). |
| **Bridge report (perspective swap)** | Optional parent nudge after dual POV; privacy-first ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.3). |
| **Life-readiness badges / profile** | Track completion badges; partner “verify” only with contracts ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.4). |
| **Ancestral / heritage story-builder** | Senior-guided family narrative + safe clone ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.5). |

---

## 8. Phased rollout

### Phase 0 — Documentation (complete)

Program docs: values, India problem map, this matrix, vision hub links.

**Product narrative sequence (orthogonal to engineering phases below):** **Trust & survival → critical thinking → emotional resilience**, with **scam + money** before heavy exam-stress fiction—see [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.4–§9.8 (“simulate reality,” named features: Trial of truth UI, scam-simulator, first salary simulation, story-interrupt, Grand-story concept, Curator’s challenge, facilitator prompts, helpline chrome).

### Phase 1 — Discovery and metadata

- **Convention (locked):** Fun vs Edu vs Simulator **theme** labels—see [admin/EDU_METADATA_CONVENTIONS.md](admin/EDU_METADATA_CONVENTIONS.md). Canonical list: [StoryCategories.kt](../backend/src/main/kotlin/com/tamixa/application/storylibrary/StoryCategories.kt).
- **Convention:** Use existing `LibraryStory.category` (and related listing fields) for **Fun vs Edu** and optional pillar slug until schema evolves; see [LibraryStory.kt](../backend/src/main/kotlin/com/tamixa/domain/LibraryStory.kt).
- **Simulator-ready metadata (when building choice content):** Chapter boundaries, **pivot** prompts, branch targets, skill tags for dashboard mapping—see [EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.6 (may require new tables or JSON alongside library stories).
- **Admin:** Document allowed values; filters on library list; validation on create/edit.
- **Clients:** Browse/filter by category and language on mobile (first) and web.

### Phase 2 — Playback UX

- Chapter marks (once storage format chosen).
- Optional co-listening / senior mode toggles.
- Parent-facing “why this episode” one-liner from metadata.

### Phase 3 — Institutional pack (school / CSR)

- **Facilitator edition:** Same IP + discussion guide + optional printable story map ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §7).
- **Activation:** Redeem codes or bulk licenses (product TBD).
- **Metrics:** Aggregate completion, language mix, optional anonymous survey—[EDUSTORY_VALUES_AND_PRINCIPLES.md](EDUSTORY_VALUES_AND_PRINCIPLES.md) §4.

### Phase 4 — Subscription and pricing (deferred)

Map SKUs to **Fun**, **Edu**, voice/avatar, and institutional seats after Phase 1–2 prove completion and trust.

---

## 10. Twelve-month narrative life simulator rollout (product + engineering)

**Assumption:** Core **story playback** (stream/audio, library, admin pipeline) is in place. This plan layers **simulation logic** and **differentiated value** without replacing the engine—aligned with [EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) **§3** and [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.

**Relation to §8 engineering phases above:** §8 **Phase 1–2** overlap **Months 1–3** here; §8 **Phase 3–4** align with **Months 11–12** and later monetization. Treat this section as the **timeboxed product roadmap**; §8 as the **technical milestone** list.

### Phase A — Interactive pivot (months 1–3)

| Area | Deliverables |
|------|----------------|
| **Goal** | Choice-based storytelling in production; **Safety Pack** live. |
| **Branching engine** | CMS supports **decision nodes**: story = ordered segments (multiple audio/text assets) chosen by user path—not a single monolithic file per title for simulator series. |
| **UI** | **Choice overlay** themed to story (e.g. chat-bubble UI for digital-safety arcs). |
| **Life bars (beta)** | Backend tracks four soft dimensions (align naming with [EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.2—e.g. wisdom, social, money, balance/emotional regulation); **author-defined** deltas per choice; **no** faux-precision parent claims. |
| **Content — Track 1** | **Scam-theater:** ~5 interactive stories × **3 regional languages** (UPI, “customer care,” deepfake/voice fraud patterns—**patterns only**; [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §10 scam safeguards). |
| **Content — Track 2** | **Money secret (teens):** ~5 stories—easy EMI traps, saving for first phone; **no investment advice**. |
| **Metric** | **Interaction rate:** share of users who complete a simulator story **hitting all choice nodes** (funnel analytics). |
| **Ecosystem (priority)** | Launch **family mission cards** + printable **decision journal** alongside scam-theater ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.9.1, §3.9 closing recommendation). |

### Phase B — Family and agency (months 4–6)

| Area | Deliverables |
|------|----------------|
| **Goal** | Reduce **generational gap** and reinforce **sales trust** (forum narratives). |
| **Two-sided mode** | **Dual role** co-play: parent vs child perspectives; **consensus gate** optional—must retain **solo completion path** ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.3). |
| **Reflection pauses** | Optional **short audio capture** (e.g. ~30s) after a dilemma—**explicit consent**, retention policy, **minors = guardian**; use for leadership/speaking tracks only if privacy-reviewed. |
| **Offline-first Edu** | Downloads / SD-friendly **Safe Pack** for low connectivity ([mobile/OFFLINE_CACHE.md](mobile/OFFLINE_CACHE.md)). |
| **Content** | Leadership/conflict (apartment, school sports); **English-anxiety** interview arc (empathy, not mockery). |
| **Marketing** | **Anti-EdTech** angle: no sales harassment, no rote stack positioning—consistent with [EDUSTORY_VALUES_AND_PRINCIPLES.md](EDUSTORY_VALUES_AND_PRINCIPLES.md) and [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9. |

### Phase C — Seniors and AI integration (months 7–10)

| Area | Deliverables |
|------|----------------|
| **Goal** | Seniors included; **high-trust** AI/voice features. |
| **Senior mode** | High contrast, large targets, slower default narration ([EDUSTORY_PRODUCT_VISION.md](EDUSTORY_PRODUCT_VISION.md) §3.1, §3.5). |
| **Voice cloning (optional)** | Consent-first clone for family narration ([VOICE_CLONING_ARCHITECTURE.md](VOICE_CLONING_ARCHITECTURE.md)). |
| **Skill dashboard (full)** | **RPG-style** growth visualization—**skill tree** (e.g. business, ethics, tech) as **themes practiced**, not medical or IQ claims. |
| **Content** | **Tech translator** metaphors (blockchain, AI, cloud); **Kirana startup** unit economics (fiction + optional sandbox from §3.4). |

### Phase D — Institutional and CSR scale (months 11–12)

| Area | Deliverables |
|------|----------------|
| **Goal** | B2B/B2G pilots: schools, NGOs, CSR. |
| **Facilitator dashboard** | **Aggregate only:** e.g. “~70% of cohort chose higher-risk path in node X”—**no** named child shaming; comply with [EDUSTORY_VALUES_AND_PRINCIPLES.md](EDUSTORY_VALUES_AND_PRINCIPLES.md) §3–§4. |
| **Custom story studio** | NGOs upload **editorially independent** local stories (health, rights)—same moderation pipeline as library content. |
| **Metrics** | Institutional partner count; **co-listening hours** (if technically measurable without invasive tracking). |

### RPG-style dashboard categories (differentiation vs marks)

| Category | What it simulates | Forum / market pain it addresses |
|----------|-------------------|----------------------------------|
| **Digital wisdom** | Scam/AI pattern recognition | Fear of being “tech-illiterate” or defrauded |
| **Social capital** | Negotiation, conflict, empathy | “Good kid” passivity; weak leadership practice |
| **Fiscal muscle** | Budgeting, debt avoidance, unit economics | EMI traps, anxiety, money taboo |
| **Cognitive agency** | Research habits, bias awareness, focus | Information overload, fake-news vulnerability |

Map these to the **four beta bars** in Phase A; **Cognitive agency** can fold into **Digital wisdom** early or split in Phase C skill tree—product decision.

### Implementation checklist (immediate)

1. **Logic audit:** Confirm player can **pause**, await **user input**, then **resume** the correct asset—if yes, branching is half the battle.
2. **Content process:** Shift simulator SKUs to **decision trees**; consider a **narrative designer** with **interactive / game writing** experience, not linear copy only.
3. **Pilot:** One ~5 min interactive **electricity-bill / utility scam** branch; test with ~10 seniors and ~10 teens—success = **empowered**, not lectured ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.5).
4. **Zero-sales protocol:** **No outbound sales use** of contact data; **no** “sales follow-up” cold calls. **OTP/phone for account/security** remains normal auth—**advertise** the anti-harassment stance only in ways that match **actual** privacy and CRM policy ([EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) §9.5).

---

## 11. Revision

Update series tables when pilots complete; update §8 when engineering ships metadata or playback features; update **§10** when month boundaries or scope change.
