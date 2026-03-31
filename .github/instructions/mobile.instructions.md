---
applyTo: "mobile/**"
---

# Mobile (KMP / Compose)

- **Mobile-first** product quality: readability, offline resilience, and **multilingual text** rendering.
- Avoid regressions in **font fallback**, **script rendering**, and **audio playback** controls.
- Keep **ViewModel** logic testable and separate from platform-specific adapters (`androidMain` / `iosMain`).
- Flag changes that affect **accessibility**, **battery**, or **bundle size**.
- Match API DTOs and naming to the backend (camelCase JSON).
