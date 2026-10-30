# Tamixa Roadmap Strategy

Product roadmap for Tamixa (kids' stories). See [README](../README.md) for project structure.

---

## Product focus (Stage 1) — ages 5–10 and life skills

Decision: [ADR-0001](adr/0001-product-focus-ages-5-10-life-skills.md) *(proposed, awaiting leadership sign-off)*.

| Area | Now | Status |
|------|-----|--------|
| **Audience** | Children aged **5–10**, through their parents | Decided (ADR-0001) |
| **Content** | Life-skills stories: feelings, honesty, sharing, friendship, safety, independence — narrated and interactive | In progress |
| **Age bands** | 5–6 / 7–8 / 9–10 picker on Home and Library; filters library rows and recommendations; remembered on device | ✅ Implemented 2026-10-23 |
| **Story generation** | Child age limited to 5–10 | ✅ Implemented 2026-10-23 |
| **Admin age groups** | Only 5–6 / 7–8 / 9–10 offered | ✅ Implemented 2026-10-23 |
| **Content re-tagging** | Every published story checked and given the right age group | ⏳ Content team |

### Later / premium (not in the current plan)

| Feature | Note |
|---------|------|
| **Avatar video** (parent face tells the story) | Parked as a possible later premium option; stays off (`AVATAR_VIDEO_ENABLED=false`). Design kept in [AVATAR_VIDEO.md](AVATAR_VIDEO.md). |

---

## PHASE 1 – Curated Library *(Current)*

### Architecture

| Component | Description |
|-----------|-------------|
| **Admin uploads** | Tamil stories uploaded via admin panel |
| **Translation service** | Multi-language support beyond Tamil |
| **TTS per language** | Text-to-speech for each supported language |
| **Passwordless login** | Auth flow without passwords (e.g. OTP/magic links) |
| **Story streaming** | Stream audio/story content to clients |
| **Subscription limits** | Plan-based limits (Free, Monthly, Yearly, Family, Voice Premium) |

### Focus Areas

- **Stability** – Reliable backend, mobile, and web
- **Performance** – Fast loading, streaming, and playback
- **UX simplicity** – Clear flows for parents and children
- **Revenue** – Subscriptions, trials, and conversion metrics

---

## PHASE 2 – Full On-Demand AI Stories *(Implemented)*

### Implemented Features

| Feature | Description | Implementation |
|---------|-------------|----------------|
| **Generate story per child** | AI-generated stories tailored to each child | When `childId` is provided: uses child's age (from DOB), language preference, and interests. Cache is skipped for personalized stories. |
| **Personalization tokens** | Child profile, preferences in generation | `Child.interests` stored and passed to prompt. Child's age and language override request params. |
| **Voice cloning** | Custom voice profiles for narration | `Story.voiceProfileId` links to parent's voice profile. Validated on generation. Audio pipeline can pass to TTS when integrated. |
| **Emotion-based bedtime modes** | Calming / soothing story modes | `emotionMode`: CALM, SOOTHING, ADVENTUROUS, DEFAULT. Influences prompt tone (e.g. "calm, soothing, perfect for bedtime"). |
| **Parent custom prompts** | Parent instructions for stories | `parentCustomPrompt` (max 200 chars, sanitized) included in generation prompt. |

### API Changes

- `POST /stories/generate`: Added optional `emotionMode`, `parentCustomPrompt`, `voiceProfileId`
- `POST /children`: Added optional `interests`
- `ChildResponse`: Added `interests`

### Mobile UI

- **Story generation**: Emotion mode filter chips (Default, Calm, Soothing, Adventure), parent instructions field
- **Add child**: Interests field for personalization

---

## Cross-References

- **Revenue & metrics** – [REVENUE_ANALYTICS.md](REVENUE_ANALYTICS.md)
- **Dashboard** – [DASHBOARD_VISUALIZATION.md](DASHBOARD_VISUALIZATION.md)
- **Compliance** – [COMPLIANCE.md](COMPLIANCE.md)
