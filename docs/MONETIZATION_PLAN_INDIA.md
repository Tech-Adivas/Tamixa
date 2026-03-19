# Tamixa Monetization Plan (India) — Final

Monetization plan for the Tamixa kids storytelling app: subscription tiers aligned with voice cloning and avatar studio, Indian market (INR), third-party API cost model, and implementation notes. **All customer-facing prices in Indian Rupee (INR).**

---

## 1. Current State Summary

**Features and gating (from codebase):**

| Feature | FREE | Paid (PREMIUM / FAMILY / VOICE_PREMIUM) |
|--------|------|----------------------------------------|
| Stories/month | 5 (config: `freeStoriesPerMonth`) | Unlimited |
| Voice generations/month | 5 (config: `freeVoiceGenerationsPerMonth`) | Tied to usage; premium catalog voices require `voicePremium` |
| Voice cloning (cloned:profileId) | Currently allowed when parentId present* | Same; no hard gate in `NarrationPremiumVoiceValidatorImpl` for cloned |
| Premium catalog voices (calm, etc.) | Blocked | Allowed when `subscription.voicePremium == true` |
| Avatar video (talking-head) | Tier says `allowsAvatarVideo=false` for FREE | PREMIUM/FAMILY tiers allow it; enforcement is tier-based where used |
| Soundscapes | No | Yes (PREMIUM/FAMILY) |
| Max children | 1 | 3 (PREMIUM), 10 (FAMILY) |

\*Cloned voice is allowed for any logged-in parent today; premium *catalog* voices are gated by `SubscriptionGuard` + `voicePremium`.

**Existing tier definitions:** `SubscriptionTierService` seeds FREE (0/0), PREMIUM (999/9999 = $9.99/$99.99), FAMILY (1999/19999). Checkout uses a single Stripe `voicePremiumPriceId`; Zoho webhook uses INR and records payment but does not set plan from product/price.

**Cost drivers:** `CostEstimatorService` (~$0.016/1k TTS chars, AI tokens); [AVATAR_VIDEO.md](AVATAR_VIDEO.md) (~$0.10/video SadTalker). Voice cloning uses ElevenLabs or self-hosted XTTS ([VOICE_CLONING_ARCHITECTURE.md](VOICE_CLONING_ARCHITECTURE.md)). **Recommendation:** Use Google Cloud TTS for default and cloned (Tamil) narration for best cost and quality; monitor per-API usage in Admin → AI metrics.

---

## 2. Third-Party API Costs (USD and INR)

**Exchange rate used:** 1 USD = ₹83 (approximate 2024–25). All per-use costs converted to INR for margin analysis.

### 2.1 API cost summary

| Provider | Use case | Unit | USD (approx) | INR/unit (₹83/USD) | Notes |
|----------|----------|------|--------------|---------------------|--------|
| **OpenAI (gpt-4o-mini)** | Story generation, rewrite, translation | 1K input tokens | $0.00015 | ₹0.012 | CostEstimatorService |
| **OpenAI (gpt-4o-mini)** | Story generation, rewrite | 1K output tokens | $0.0006 | ₹0.050 | |
| **Default TTS** (e.g. Google/Neural) | Default narration (no clone) | 1K characters | $0.016 | ₹1.33 | Existing estimate in codebase |
| **ElevenLabs (Flash/Turbo)** | Cloned-voice TTS | 1K characters | $0.06–0.08 | ₹5.0–6.6 | Business/API tier; Multilingual v2 ~$0.12/1k = ₹10/1k |
| **ElevenLabs** | Instant Voice Cloning (add voice) | One-time per profile | Included in usage | — | Consumes character quota when synthesizing |
| **Replicate (SadTalker)** | Avatar talking-head video | Per prediction | $0.10 | ₹8.30 | Cached per story/parent/language/voice |
| **HeyGen** (optional) | Premium avatar | Per video | Higher than SadTalker | — | Use if quality justifies; not in base model |
| **Self-hosted XTTS** | Cloned voice (fallback) | Per request | Infra (GPU) only | — | No per-char API; use for Hindi/non-Tamil to reduce ElevenLabs spend |

### 2.2 Per-story cost assumptions (INR)

Assume one “story narration” = one play in one language with one voice:

- **Script length:** ~2,500 characters TTS, ~1.5K input + ~0.8K output tokens (format/rewrite/generation).
- **Default (no clone):** AI ≈ ₹0.02 + ₹0.04 = ₹0.06; TTS 2.5×1.33 ≈ ₹3.33 → **~₹3.4 per story** (default voice).
- **Cloned voice (ElevenLabs):** Same AI; TTS 2.5×5.5 ≈ ₹13.75 → **~₹14 per story** (first-time; cached replays = ₹0).
- **Avatar (Replicate):** **₹8.30 per new video** (cached replays = ₹0).

So: **FREE/STARTER** cost ≈ ₹3.4 × stories. **PREMIUM/FAMILY** add ElevenLabs (cloned) and Replicate (avatar); blended cost depends on % of stories with clone and % with new avatar.

### 2.3 Monthly cost per tier (illustrative)

| Tier | Stories (cap) | Cloned plays (new) | New avatar videos | Est. API cost/month (INR) |
|------|----------------|--------------------|-------------------|---------------------------|
| FREE | 5 | 0 | 0 | 5 × 3.4 ≈ **₹17** |
| STARTER | 15 | 0 | 0 | 15 × 3.4 ≈ **₹51** |
| PREMIUM | 30 (assumed avg) | 15 (half new) | 10 (cap) | 30×3.4 + 15×10.6 + 10×8.3 ≈ **₹102 + ₹159 + ₹83 = ₹344** |
| FAMILY | 60 (assumed avg) | 30 | 30 (cap) | 60×3.4 + 30×10.6 + 30×8.3 ≈ **₹204 + ₹318 + ₹249 = ₹771** |

(Cloned “new” cost ≈ ₹14 − ₹3.4 = ₹10.6 incremental per story when ElevenLabs is used; avatar ₹8.3 each.) These are conservative mid-range usage assumptions; heavy users can exceed, so caps (e.g. `maxAvatarVideos`) protect margin.

---

## 3. India Market: Expectations and Price Sensitivity

- **Willingness to pay:** Parents pay for education and “premium experience” in the ₹99–₹499/month range for apps (e.g. learning apps, music). Annual is often 50–60% of monthly×12 to reduce churn.
- **Expectations:** Strong free tier to try (stories + limited personalization), then clear value for paid: “your voice / your face” (voice cloning + avatar) is a strong differentiator; family plans (multiple kids) are expected.
- **Payments:** UPI/cards; Zoho already integrated with INR; Stripe supports INR but Zoho/Razorpay-style flows are familiar in India.
- **Positioning:** Voice cloning and avatar studio should be the **flagship paid differentiators**, not buried in a single “voice premium” SKU.

---

## 4. Pricing Strategies (Analysis)

Four approaches, with INR implications and trade-offs.

### Strategy A: Cost-plus (cover API + margin)

- **Idea:** Price each tier so that estimated API cost per user is a fraction of revenue (e.g. 25–35% cost, rest margin + fixed costs).
- **From section 2.3:** FREE ₹17, STARTER ₹51, PREMIUM ₹344, FAMILY ₹771. At 30% cost share: STARTER ≥ ₹170/mo, PREMIUM ≥ ₹1,147/mo, FAMILY ≥ ₹2,570/mo.
- **Pros:** Sustainable unit economics; safe at scale.
- **Cons:** STARTER/PREMIUM at ₹170/₹1,147 are high for India; likely low conversion.

### Strategy B: Value-based (what market bears)

- **Idea:** Price at willingness to pay; accept thin or negative margin on some tiers and compensate with volume and upsell.
- **Typical India bands:** STARTER ₹49–99, PREMIUM ₹199–349, FAMILY ₹399–599.
- **Pros:** Strong conversion and trial→paid; good for growth.
- **Cons:** PREMIUM at ₹249 with ~₹344 cost is loss-making unless usage is capped or average usage is lower than assumed; need strict caps (e.g. max stories with clone, max avatar videos).

### Strategy C: Penetration (aggressive acquisition)

- **Idea:** Very low entry (e.g. STARTER ₹49, PREMIUM ₹149) to maximise installs and rely on FAMILY and future price increases.
- **Pros:** Fast user growth; network effects if you add social/sharing later.
- **Cons:** High burn; requires funding or other revenue; STARTER/PREMIUM both below cost at assumed usage.

### Strategy D: Hybrid (recommended)

- **Idea:** Keep FREE and STARTER low for acquisition; price PREMIUM and FAMILY to cover blended API cost with a target margin, and enforce usage caps so that “average” user stays within budget.
- **Mechanics:**
  - FREE: 5 stories (≈₹17 cost); no voice clone/avatar.
  - STARTER: ₹99/mo, 15 stories (≈₹51 cost); margin ~₹48 before fixed costs.
  - PREMIUM: ₹299/mo (not ₹249); cap e.g. 20 new cloned narrations + 10 avatar videos/month so expected cost ≈ ₹210 + ₹83 = ₹293; margin thin but non-negative; unlimited “cached” replays.
  - FAMILY: ₹549/mo; cap e.g. 40 cloned + 40 avatar; expected cost ≈ ₹424 + ₹332 = ₹756 → still above revenue at 40 avatar, so cap avatar at 25 or price at ₹599.
- **Refinement:** Either (1) raise PREMIUM to ₹349 and FAMILY to ₹599 with current caps, or (2) keep PREMIUM ₹249/₹1,999 but tighten caps (e.g. 8 avatar, 12 new cloned/month) so average cost ≈ ₹200 and margin ~₹49.

---

## 5. Final Recommended Strategy (INR Only)

After weighing acquisition (Strategy B/C) and sustainability (Strategy A/D), the **final recommendation is Hybrid (D)** with **INR-only pricing for India** and **usage caps** so third-party API cost stays under control.

### 5.1 Final tier and price matrix (INR)

| Tier | Monthly (INR) | Yearly (INR) | Stories | Voice cloning | Avatar studio | Caps to protect margin |
|------|----------------|--------------|---------|----------------|---------------|-------------------------|
| **FREE** | ₹0 | — | 5/month | No | No | — |
| **STARTER** | ₹99 | ₹799 (~₹66/mo) | 15/month | No | No | — |
| **PREMIUM** | ₹299 | ₹2,399 (~₹200/mo) | Unlimited | Yes (3 voices) | Yes | 15 new cloned narrations/mo, 10 avatar videos/mo |
| **FAMILY** | ₹549 | ₹4,399 (~₹367/mo) | Unlimited | Yes (10 voices) | Yes | 30 new cloned/mo, 25 avatar videos/mo |

- **Why ₹299 (not ₹249):** At ₹249, PREMIUM is below estimated API cost for moderate usage; ₹299 with caps keeps average cost ~₹250–280 and leaves ~₹20–50 margin before fixed costs.
- **Why ₹549 for FAMILY:** Covers higher blended cost (more stories + clone + avatar) and 25 avatar cap keeps Replicate spend ~₹208; total estimated cost ~₹550–600 so margin is thin but non-negative.
- **Storage:** Store in minor units (paise): 29900, 239900, 54900, 439900. Currency = INR in config/tier.

### 5.2 Third-party cost check (final)

- **STARTER:** 15 × ₹3.4 ≈ ₹51; revenue ₹99 → **~48% cost**.
- **PREMIUM:** With caps, ~₹250–280; revenue ₹299 → **~85–95% cost**, small margin.
- **FAMILY:** With caps, ~₹500–600; revenue ₹549 → **~91–109%**; if needed, cap avatar at 20 (₹166) to keep total cost ~₹520.

Adjust caps in product/backend (e.g. `maxAvatarVideos` per period, “new cloned narrations” counter) so that typical users stay within these bounds; monitor CostEstimatorService and Replicate/ElevenLabs usage and tweak caps or price once real data is available.

### 5.3 Summary

- **All amounts in Indian Rupee (INR);** no USD in customer-facing prices for India.
- **Third-party APIs** (OpenAI, default TTS, ElevenLabs, Replicate SadTalker) are included in the cost model; **ElevenLabs and Replicate** are the main variable costs for PREMIUM/FAMILY.
- **Strategy:** Hybrid — value-based entry (FREE, STARTER), cost-aware PREMIUM/FAMILY with caps and ₹299/₹549 so that, on average, API cost is covered and margin is positive or near break-even with room to optimize after launch.

---

## 6. Tier Reference (Quick View)

| Tier | Monthly (INR) | Yearly (INR) | Stories | Voice cloning | Avatar studio | Children | Target |
|------|----------------|--------------|---------|----------------|---------------|----------|--------|
| **FREE** | 0 | — | 5/month | No | No | 1 | Trial, acquisition |
| **STARTER** | 99 | 799 (~₹66/mo) | 15/month | No | No | 1 | Light payers |
| **PREMIUM** | 299 | 2,399 (~₹200/mo) | Unlimited | Yes (3 voices) | Yes (10/mo) | 3 | Voice + avatar |
| **FAMILY** | 549 | 4,399 (~₹367/mo) | Unlimited | Yes (10 voices) | Yes (25/mo) | 10 | Multiple kids |

**Pricing storage:** Backend stores amounts in **minor units** (paise). Tiers have `priceMonthly` / `priceYearly` in `SubscriptionTier`; use same convention and add currency (INR) in config or tier metadata. Seed/migration: FREE 0/0, STARTER 9900/79900, PREMIUM 29900/239900, FAMILY 54900/439900.

---

## 7. Feature Gating Recommendations

- **Voice cloning:** Gate by plan: only PREMIUM and FAMILY. Add a check in `NarrationPremiumVoiceValidatorImpl` or stream/playback path: require `subscription.plan in [PREMIUM_MONTHLY, PREMIUM_YEARLY, FAMILY]` (or tier `allowsVoiceCloning`) for `voiceProfile.startsWith("cloned:")`.
- **Avatar video:** Gate by tier `allowsAvatarVideo` and enforce `maxAvatarVideos` per period. Ensure AvatarVideoService / stream URL path checks subscription tier before generating or serving avatar video.
- **FREE:** Keep 5 stories and 5 voice generations; no voice cloning, no avatar.
- **STARTER:** 15 stories/month, no voice cloning, no avatar (or 1 “taste” avatar if desired as hook).

---

## 8. Marketing Perspective

- **Messaging:** “Stories in your voice” and “You in the story” (avatar) as the main paid hooks; STARTER as “more stories” for hesitant payers.
- **Free tier:** Enough to complete a few stories and see value; limit voice/avatar to create upgrade moment.
- **Trials:** 7-day trial on PREMIUM (existing `trial-days`, `trialEnd`) to let parents try voice + avatar; avoid trial on STARTER to keep it simple.
- **Channels:** App store optimization (India), short demos of voice cloning + avatar on social, parent communities and schools.
- **Objection handling:** “Is my data safe?” — point to existing consent/export and privacy; “Worth ₹299?” — frame as cost per story (unlimited) and emotional value of “your voice reading to your child.”

---

## 9. Infrastructure and Ops Constraints

- **Cost control:** Per-story cost (AI + TTS) estimated in CostEstimatorService. Cap avatar generations per tier (`maxAvatarVideos`); cache aggressively (already done per story/parent/language/voice). Voice cloning: ElevenLabs has per-character cost; self-hosted XTTS reduces marginal cost but needs GPU/reliability. Monitor usage per plan.
- **Rate limits:** AppProperties has `storyGenerationFreePerHour` (10) and `storyGenerationPaidPerHour` (60). Consider separate limits for voice cloning and avatar generation (e.g. per day per tier) to prevent abuse.
- **Payments:** **India:** Prefer Zoho (already integrated, INR) for INR products; ensure webhook sets `plan` and `voicePremium` from payment link/product. **International:** Keep Stripe for USD/other currencies; support multiple Stripe Price IDs (STARTER, PREMIUM monthly/yearly, FAMILY) and map product/price in webhook to SubscriptionPlan.
- **Grace and trials:** Existing `gracePeriodDays` (3) and trial expiry jobs are sufficient; ensure Zoho payment failure flow updates subscription to PAST_DUE/EXPIRED where applicable.

---

## 10. Other Constraints (Compliance, Localization, Product)

- **Compliance:** No PII in logs (existing rules); store only necessary billing data; consent for marketing if sending receipts/promos.
- **Tax:** GST in India; ensure invoicing (Zoho/Stripe) supports GST number and tax-inclusive or -exclusive display as per your policy.
- **Localization:** App and pricing in English and optionally Hindi for India; Tamil/other languages already in product (stories/TTS). Show INR and “₹” by default when region/currency is India.
- **App store:** Google Play billing for in-app subscriptions in India; consider offering IAP alongside web-based Zoho/Stripe for users who prefer billing through the store (revenue share 15–30%).

---

## 11. Implementation Checklist (High Level)

- **Config and tiers:** Add STARTER plan and tier with INR amounts in paise. Extend SubscriptionTierService / seed or migration: FREE 0/0, STARTER 9900/79900, PREMIUM 29900/239900, FAMILY 54900/439900. Add currency to tier or app config (e.g. `defaultCurrency: INR` for India).
- **Checkout:** Zoho: create payment links per plan (STARTER, PREMIUM_Monthly, PREMIUM_Yearly, FAMILY_Monthly, FAMILY_Yearly); pass `parent_id` and plan in metadata; on `payment_link.paid`, upsert subscription with correct plan and set `voicePremium` and `maxChildren` from plan. Stripe: add Price IDs for each product; StripeCheckoutAdapter to accept `plan` and create session with that price; webhook already reads `metadata.plan` and `voice_premium` — ensure products set these.
- **Gating:** Enforce voice cloning by plan/tier (PREMIUM/FAMILY) in narration/stream path. Enforce avatar by tier and `maxAvatarVideos` usage.
- **Usage:** If STARTER has 15 stories/month, enforce via existing UsageTracking and plan-specific limit in config or tier. Optionally track “avatar videos generated” per parent per month and cap by tier.
- **Admin and analytics:** Revenue dashboard already has plan and MRR; ensure plans (FREE, STARTER, PREMIUM, FAMILY) and currency (INR) are visible; track conversion FREE→STARTER, STARTER→PREMIUM, and trial→paid.

---

## 12. Summary: Best Price Plan Options for India (Final)

| Option | Monthly (INR) | Yearly (INR) | Best for |
|--------|----------------|--------------|----------|
| **FREE** | ₹0 | — | Trial, 5 stories/month |
| **STARTER** | ₹99 | ₹799 | “More stories,” low commitment |
| **PREMIUM** | ₹299 | ₹2,399 | Voice cloning + avatar studio (main revenue) |
| **FAMILY** | ₹549 | ₹4,399 | Multiple children, heavy users |

**All prices in Indian Rupee (INR).** Third-party API costs (OpenAI, ElevenLabs, Replicate SadTalker) are reflected in Section 2; PREMIUM and FAMILY use usage caps so that average cost stays within margin (Section 5). Implement by: (1) defining tiers with INR in paise (e.g. 29900, 239900), (2) gating voice cloning and avatar to PREMIUM/FAMILY and enforcing caps, (3) using Zoho for INR checkout and webhooks so plan and entitlements are set correctly after payment.

---

## 13. Best Strategy to Succeed (Investigation-Based)

This section is based on a deeper pass over the codebase and product flow. It lists critical gaps, then a phased success strategy.

### 13.1 Critical gaps (must fix for conversion)

- **Zoho webhook does not set plan after payment.** Today `handleZohoPaymentSucceeded` and `handleZohoPaymentLinkPaid` only call `recordPaymentSucceeded`. They do **not** call `upsertFromProvider` (or equivalent) to set `plan`, `voicePremium`, or `maxChildren`. So after a successful INR payment the user stays on FREE and does not get unlimited stories or voice/avatar. **Action:** In the Zoho webhook, derive plan from payment link ID or metadata (e.g. `plan=PREMIUM_MONTHLY`) and call a method that creates/updates the subscription with that plan and entitlements (e.g. `upsertFromProvider` with plan, maxChildren, voicePremium from a plan lookup).
- **Free story limit returns generic error, not upgrade CTA.** `StoryService.enforceStoryEntitlement` throws `FreeStoryLimitReachedException`, which carries `limit`, `remaining`, `recommendedPlan`. There is **no** `@ExceptionHandler` in `GlobalExceptionHandler` for this exception. The API therefore returns 500 and a generic message; the mobile shows “Failed to generate story” and does not get structured `LimitReachedResponse` (status `LIMIT_REACHED`, `upgradeRequired`, `recommendedPlan`). **Action:** Add an exception handler that returns HTTP 402 with `LimitReachedResponse` body. On mobile, when story generate fails with 402 or response body `status === "LIMIT_REACHED"`, show a dedicated “You’ve used your 5 free stories — upgrade to get more” nudge and a button that navigates to Subscription (or checkout).
- **Voice cloning not gated by plan.** `NarrationPremiumVoiceValidatorImpl` allows cloned voice (`voiceProfile.startsWith("cloned:")`) whenever `parentId != null`, so FREE users can use clone today. **Action:** Require subscription plan in PREMIUM/FAMILY (or tier `allowsVoiceCloning`) for cloned voice and enforce in the same validator or in the stream URL path so that FREE/STARTER see an upgrade prompt when selecting cloned voice (consistent with premium catalog voice 402 flow).
- **Usage visibility before limit.** The subscription/usage API is used on the Subscription screen, but the Story Generation screen does not show “X / 5 stories used this month” before the user taps Generate. **Action:** On Story Generation (and optionally Dashboard), show current usage and limit for FREE/STARTER so the paywall moment is expected; consider a soft nudge at 4/5 (e.g. “One more free story left — upgrade for unlimited”).

### 13.2 Conversion funnel (order of operations)

1. **First value in &lt; 2 minutes.** Onboarding: minimal steps (register → add child → one tap to first story or first generation). Curated stories can deliver value without generation; first AI story should be possible within 5 taps from open.
2. **Paywall at limit, not at door.** Do not block voice/avatar behind paywall before the user has tried default stories. After 5 stories (FREE), hit the limit and show the 402/LIMIT_REACHED flow with clear CTA: “Unlimited stories + your voice + your face — from ₹299/mo.”
3. **Trial on PREMIUM only.** Offer 7-day trial when upgrading to PREMIUM (already supported: `trialEnd`, trial expiry job). Do not offer trial on STARTER so STARTER stays a simple “more stories” purchase. Restrict one trial per account (already: `trialUsed`).
4. **STARTER as step-up.** Position STARTER (₹99) for users who balk at ₹299: “15 stories/month, no voice/avatar.” Later email or in-app nudge: “Add your voice and face — upgrade to Premium.”
5. **Checkout must reflect plan.** Zoho payment links must be per plan (STARTER, PREMIUM_Monthly, PREMIUM_Yearly, FAMILY_Monthly, FAMILY_Yearly) with `parent_id` and `plan` in metadata/reference; webhook must set subscription plan and entitlements so the user sees unlimited (and voice/avatar for PREMIUM/FAMILY) immediately after payment.

### 13.3 Retention and caps

- **Communicate caps clearly.** In Subscription and Settings, show “X / 10 avatar videos this month” for PREMIUM so users understand the cap; same idea for “new cloned narrations” if you cap that. Avoid surprise “you’ve reached your limit” with no prior visibility.
- **Grace and retry.** Keep existing 3-day grace period and trial expiry; ensure Zoho payment-failure path (if you add it) moves subscription to PAST_DUE and then EXPIRED after grace so users are not left in a broken state.
- **Win-back.** After EXPIRED, still show “Renew to get back unlimited stories and your voice” with a single tap to the same Zoho link (no re-onboarding).

### 13.4 India-specific execution

- **Currency and trust.** All store and in-app prices in INR; “₹299/month” not “$3.6”. Privacy policy and data handling (no PII in logs, consent, export) visible in app and on store listing.
- **Payments.** Use Zoho for INR; ensure UPI/cards work and that success redirect returns to app with a “Thank you — you’re now Premium” state (subscription API returning plan PREMIUM and unlimited).
- **ASO and positioning.** Keywords: kids stories, bedtime stories, Tamil/Hindi stories, personalized stories, voice clone, storytelling. Short video showing “your voice reading to your child” and “you in the story” (avatar) for PREMIUM.
- **Channels.** Parent groups, schools, and “story in your language” (Tamil/Hindi) as differentiators; consider a small referral incentive (e.g. one extra week Premium for referrer and referee) once base flow is stable.

### 13.5 Metrics and iteration

- **Track:** Trial start, trial→paid conversion, FREE→STARTER, STARTER→PREMIUM, free-limit hits (count of 402/LIMIT_REACHED or equivalent), MRR by plan, churn by plan. Revenue dashboard already has trial count and trial conversion rate; ensure Zoho-sourced subscriptions are included in MRR and plan distribution once webhook sets plan.
- **Iterate.** If trial conversion is low, test trial length (e.g. 3 vs 7 days) or in-trial reminder (“2 days left — add your voice before it’s gone”). If FREE→paid is low, test soft paywall at 4/5 stories or a one-time “bonus” 3 stories for signing up. If PREMIUM cost exceeds revenue, tighten caps (e.g. 8 avatar, 12 new cloned/mo) and re-measure; update CostEstimatorService and Replicate/ElevenLabs usage so caps are data-driven.

### 13.6 Phased rollout

- **Phase 1 (launch):** FREE (5 stories) + PREMIUM (₹299/₹2,399) with 7-day trial. Fix Zoho webhook (set plan), add 402 + LimitReachedResponse for story limit, gate cloned voice to PREMIUM, and show upgrade CTA on limit. No STARTER yet to keep messaging simple (“Upgrade to Premium for unlimited + your voice + your face”).
- **Phase 2:** Add STARTER (₹99/₹799, 15 stories, no voice/avatar) for users who want more stories but not full Premium. Add usage on Story Generation screen. Optionally add FAMILY (₹549/₹4,399) if demand appears (e.g. from support or analytics).
- **Phase 3:** Add FAMILY if not in Phase 2; refine caps and pricing from real usage and cost data; consider referral or limited-time annual discount (e.g. first year ₹1,999) to push annual commits.

Implementing the critical fixes (webhook→plan, 402 + limit CTA, voice cloning gating, usage visibility) and the funnel above gives the best chance of success for the India monetization plan.
