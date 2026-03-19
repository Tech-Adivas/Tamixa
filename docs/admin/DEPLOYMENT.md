# Tamixa Admin Dashboard – Deployment Guide

## Prerequisites

- Node.js 20+
- Backend API running (Tamixa Kotlin/Spring Boot) with at least one user with role `ADMIN`
- Environment variables for the admin app (see below)

## Environment-based config

Create `.env.local` (or set in your host) from `.env.example`:

```bash
cp .env.example .env.local
```

| Variable | Description | Example |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API base URL (no trailing slash) | `http://localhost:8080` or `https://api.tamixa.example.com` |

- **Development:** Backend usually at `http://localhost:8080`. Ensure CORS on the backend allows the admin origin (e.g. `http://localhost:3000`).
- **Production:** Set `NEXT_PUBLIC_API_URL` to your public API URL. Use HTTPS.

## Local run

```bash
cd admin
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Use “Admin login” and sign in with an **ADMIN** account (created in the backend).

## Docker build and run

Build:

```bash
cd admin
docker build -t tamixa-admin:latest .
```

Run (example with env):

```bash
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_API_URL=http://host.docker.internal:8080 \
  tamixa-admin:latest
```

For production, point `NEXT_PUBLIC_API_URL` to your real API (e.g. `https://api.tamixa.example.com`). **Note:** `NEXT_PUBLIC_*` are baked in at build time, so pass them at `docker build` if you need build-time config:

```bash
docker build --build-arg NEXT_PUBLIC_API_URL=https://api.tamixa.example.com -t tamixa-admin:latest .
```

You’ll need to add `ARG NEXT_PUBLIC_API_URL` in the Dockerfile build stage and pass it into the build if you use build-time API URL.

## Security checklist

- **Admin only:** Only users with role `ADMIN` can access dashboard routes; others are redirected to login.
- **JWT:** Access and refresh tokens are stored in `localStorage`; refresh is used when the access token expires.
- **Audit:** Admin actions (e.g. flag story) are logged on the backend (AUDIT log).
- **Backend:** Rate limiting and role validation are enforced by the backend; keep JWT secret and API URL secure.

## Clean component structure

- `src/app/(auth)/login` – Login page
- `src/app/(dashboard)/*` – All admin pages (dashboard home, parents, children, stories, voice-logs, subscriptions, moderation, health, kafka, ai-metrics, audit)
- `src/components/layout/` – Sidebar, header, dashboard guard
- `src/components/ui/` – shadcn/ui components
- `src/contexts/auth-context.tsx` – Auth state and role
- `src/lib/api.ts` – REST API client and JWT refresh
- `src/types/api.ts` – Shared API types

## Backend requirements

- `POST /api/v1/auth/login` – Returns `accessToken`, `refreshToken`, `expiresInSeconds`
- `GET /api/v1/auth/me` – Returns `{ email, role }` (must be `ADMIN` for dashboard)
- `POST /api/v1/auth/refresh` – Body `{ refreshToken }`, returns new tokens
- Admin endpoints under `GET/POST /api/v1/admin/*` (see backend `AdminController`), all require `Authorization: Bearer <accessToken>` and role `ADMIN`.

Ensure at least one user has `ADMIN` role in the database for logging in.
