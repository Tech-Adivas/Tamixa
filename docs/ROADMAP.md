# Tamixa Roadmap Strategy

Product roadmap for Tamixa (kids' stories). See [README](../README.md) for project structure.

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
