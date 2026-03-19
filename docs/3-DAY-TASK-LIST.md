# Tamixa Kids – 3-Day Completion Task List

## ✅ Day 1 (Completed)

### Backend
- [x] Parent suspend endpoint (already existed)
- [x] Story approve/reject endpoints (already existed)
- [x] Invoices & refund endpoints (already existed)
- [x] **Added**: `GET /api/v1/admin/stories/{id}` – story detail with content for moderation preview
- [x] **Added**: Parent list status filter (`?status=ACTIVE|SUSPENDED`)

### Admin Dashboard
- [x] Dashboard KPIs – wired to real API when `NEXT_PUBLIC_USE_MOCK_API` is not set
- [x] Parent suspend – calls real API
- [x] Story approve/reject/flag – calls real API
- [x] **Added**: Story preview loads full content from `GET /api/v1/admin/stories/{id}`
- [x] **Added**: Parents page status filter passed to backend
- [x] Sidebar – Stories, Children, Voice logs, Health, AI metrics, Kafka, Audit already linked

---

## ✅ Day 2 (Completed)

### Mobile
- [x] Audio playback – ExoPlayer wired to stream URLs (already integrated)
- [x] **Added**: Release build uses production BASE_URL via `TAMIXA_API_BASE_URL` gradle property (default: `https://api.tamixa.com`)

### Build
- [x] Run release build: `./gradlew :composeApp:assembleRelease -PTAMIXA_API_BASE_URL=https://your-api.com`

---

## Day 3 – Deployment Prep

### Pre-deploy checklist
- [ ] Run Flyway migrations on production DB
- [ ] Set production env vars (Backend, Web, Admin)
- [ ] Verify Dockerfile builds
- [ ] Configure SendGrid (magic link) and Twilio (OTP) or use dev bypass
- [ ] Set `CDN_STREAM_ENABLED=true` and GCP bucket for production audio

### E2E smoke tests
- [ ] Web: Register → Login → Create child → Generate story → Play audio
- [ ] Mobile: Login → Dashboard → Play story
- [ ] Admin: Parents list → Suspend; Moderation → Approve/Reject

---

## Quick reference

| Component   | Production config                          |
|------------|---------------------------------------------|
| Backend    | `SPRING_PROFILES_ACTIVE=prod`, DB, Redis, Kafka |
| Web        | `VITE_API_URL=https://api.tamixa.com`        |
| Admin      | `NEXT_PUBLIC_API_URL=https://api.tamixa.com` (do NOT set `USE_MOCK_API`) |
| Mobile     | `-PTAMIXA_API_BASE_URL=https://api.tamixa.com` in release build |
