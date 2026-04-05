# Edu-Simulator — Tamixa content blueprint

**Purpose:** Authoring guide for **interactive decision-tree** episodes (not linear “moral stories”). You are designing **lived consequences**, not lectures.  
**Taxonomy & JSON shape:** [EDU_METADATA_CONVENTIONS.md](EDU_METADATA_CONVENTIONS.md) (`Learn · Simulator · …`, `interactive_graph`).  
**Product context:** [EDUSTORY_PRODUCT_VISION.md](../EDUSTORY_PRODUCT_VISION.md), [EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md](../EDUSTORY_CONTENT_MATRIX_AND_ROADMAP.md), [EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md](../EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md) (evidence → scenario seeds), [EDUSTORY_VALUES_AND_PRINCIPLES.md](../EDUSTORY_VALUES_AND_PRINCIPLES.md).

---

## Core principle

Content preparation for an Edu-Simulator is **fundamentally different** from writing a moral tale. The user **lives through outcomes** on a **decision tree**—each node is a fork, each branch shows what happens next.

---

## The Tamixa blueprint (every story)

Before drafting, structure every episode in **four steps**:

1. **The relatable hook** — An India-specific situation (wedding, school fest, housing society, neighborhood dispute, family group panic).
2. **The dilemma (the node)** — The character must choose. Choices should **not** be “right vs wrong”; frame them as **short-term gain vs long-term wisdom** (both can feel defensible in the moment).
3. **The narrative fallout** — **Show**, don’t tell. If the player picks a costly path, **do not** say “you failed.” Show stress, lost money, damaged trust, time lost, or embarrassment—**concrete**, not preachy.
4. **The reflection / mission** — One prompt for the family to discuss (optional challenge tone, not homework—see [EDU_METADATA_CONVENTIONS.md](EDU_METADATA_CONVENTIONS.md) §6).

---

## Category 1: Technology & digital safety (digital wisdom)

**Focus:** **Pattern recognition**, not only “don’t click links.”

**Scenario ideas:**

- **The “electricity bill” urgent call** — A senior gets a call: power will be cut in one hour unless an app is installed.
- **The “deepfake” request** — A teen gets a video call from a “friend” asking for an emergency UPI transfer.

**Decision nodes (example fork):**

| Path | Stance |
|------|--------|
| **A** | Panic and follow instructions (high-stress path). |
| **B** | Question the caller’s identity (analytical path). |
| **C** | Ask a tech-savvy family member (collaborative path). |

**Content goal:** Teach recognition of **urgency** and **secrecy** as two common hallmarks of scams and manipulation.

**Metadata:** Use **`Learn · Simulator · Digital Safety`** when the episode is branching (see conventions doc).

---

## Category 2: Money & finance (fiscal muscle)

**Focus:** Psychology of money—debt, inflation, FOMO, face-saving.

**Scenario ideas:**

- **The “no-cost” EMI trap** — Latest phone before a college trip; “easy EMIs.”
- **The “friend-loan” dilemma** — A childhood friend asks for money for a “crypto business.”

**Decision nodes (example fork):**

| Path | Stance |
|------|--------|
| **A** | Buy now to save face (FOMO / status path). |
| **B** | Save for six months and buy the older model (patience path). |

**Content goal:** Show that money is about **time and freedom**, not only numbers on a screen.

**Metadata:** Future lane e.g. **`Learn · Simulator · Money`**—confirm string in `StoryCategories` before publishing.

---

## Category 3: Leadership & social capital (soft skills)

**Focus:** Boundaries, negotiation, empathy—moving from “log kya kahenge” to **how we solve this together**.

**Scenario ideas:**

- **Housing society conflict** — Teen wants to play football; elderly residents object; negotiate a schedule.
- **Group project free-rider** — One student does all the work; address peers without torching friendships.

**Decision nodes (example fork):**

| Path | Stance |
|------|--------|
| **A** | Aggressive confrontation (burns bridges). |
| **B** | Passive silence (burnout path). |
| **C** | Facilitated dialogue (leadership path). |

**Content goal:** **Collaborative problem-solving** under social pressure.

**Metadata:** Future lane e.g. **`Learn · Simulator · Conflict`**—align with product before use.

---

## Category 4: Business & entrepreneurship (resourcefulness)

**Focus:** Unit economics and customer empathy—not “getting funded” fantasy.

**Scenario ideas:**

- **Home-baker scale-up** — First ₹5000: Instagram ads vs better ingredients?
- **Tech-repair side hustle** — Customer claims you broke a screen that was already cracked; “customer is always right” pressure.

**Content goal:** Business as **solving a problem for a profit**, with trade-offs visible.

---

## Category 5: Public speaking & confidence (the voice)

**Focus:** **Authenticity** over perfect English.

**Scenario ideas:**

- **Assembly speech panic** — Speech in English; fear of accent.
- **Job interview shadowing** — Dream job; question you don’t know how to answer.

**Decision nodes (example fork):**

| Path | Stance |
|------|--------|
| **A** | Big words and faked accent (inauthentic path). |
| **B** | Simple language; admit gaps honestly (integrity path). |

**Content goal:** Normalize mistakes; **clarity** over performative fluency.

---

## Content pipeline — action steps

1. **Build a conflict library** — Seed scenarios from real dilemmas (e.g. r/India, r/TwoXIndia, r/LegalAdviceIndia—use as **inspiration**, verify tone and age fit for Tamixa).
2. **Write the choice matrix first** — Before prose, map: Choice A → segment / outcome; Choice B → … (mirrors `interactive_graph` in admin).
3. **Grandma / grandchild test** — Would a ~12-year-old find it engaging? Would a ~65-year-old find it useful? If both, you are in Tamixa range.
4. **Regional adaptation (not just translation)** — Hindi → Tamil (etc.) may change setting and cultural detail (chai stall vs filter coffee, society norms)—same lesson, **local texture**.
5. **Family mission draft** — End with **one sentence** parents and kids can use tonight.  
   *Example (money):* “Ask your parents about the first thing they ever bought with their own salary.”

---

## Immediate recommendation

**Start with one track: “Digital survival guide.”** It matches the highest recurring demand in Indian forums and maps cleanly to **`Learn · Simulator · Digital Safety`**. Mastering **scam-theater** narrative (urgency, secrecy, tempting wrong taps) sets the template for money, leadership, and other simulators. **Sample Episode 1 graph (JSON) + 15 pilot outlines:** [DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md](DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md).

---

## Revision

When product adds new **`Learn · Simulator · …`** lanes, update [EDU_METADATA_CONVENTIONS.md](EDU_METADATA_CONVENTIONS.md) §2–§3 and backend/admin/mobile category lists together.
