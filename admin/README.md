# Tamixa Admin Dashboard

Next.js 14 (App Router) production-ready admin dashboard for Tamixa. **Admin-only** access with JWT auth and role validation. Brand: warm gold accent, deep maroon highlights, clean enterprise layout.

## Tech stack

- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS**
- **shadcn/ui** (Radix primitives)
- **JWT** (access + refresh), **ADMIN** role required
- **REST API** + **mock API** (when `NEXT_PUBLIC_USE_MOCK_API=true` or for demo)

## Features

1. **Admin login** – Email/password, role validation, centered card layout
2. **Dashboard overview** – KPI cards (subscriptions, revenue, story generations, AI tokens, moderation flags), revenue chart, story usage trend chart
3. **Parent management** – Table (name, email, plan, status, created), search, filter by status, pagination, suspend action
4. **Story moderation** – List by status (READY / FLAGGED / FAILED), story preview modal, Approve / Reject / Flag, audit indicator
5. **Subscription management** – Plan breakdown, subscription table, invoice table with payment status, refund (mock)
6. **Story library** – Upload story text; audio is generated via TTS pipeline. See [Story pipeline flow](../docs/admin/STORY_PIPELINE_FLOW.md) for when the pipeline runs (Approve for delivery runs pipeline; Run pipeline; Regenerate audio).
7. **System monitoring** – API latency, Kafka lag, Redis hit ratio, error rate, AI cost tracking

## Security

- Only users with role **ADMIN** can access dashboard routes
- JWT stored in `localStorage`; refresh used on 401
- Admin actions (e.g. flag story) logged via backend AUDIT log
- Pagination and filtering on all list endpoints
- Backend enforces rate limiting and role checks

## Quick start

```bash
npm install
cp .env.example .env.local
# Set NEXT_PUBLIC_API_URL to your backend (e.g. http://localhost:8080)
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) → **Admin login**. Use an account that has **ADMIN** role in the backend.

To use **mock data** for dashboard KPIs, charts, invoices, and system monitoring (e.g. when backend is unavailable), set in `.env.local`:

```
NEXT_PUBLIC_USE_MOCK_API=true
```

Login still requires your backend to issue a JWT for an **ADMIN** user; mock data then powers dashboard overview, monitoring, and list pages where the API is not called.

## Project structure

```
src/
├── app/
│   ├── (auth)/login/          # Login page
│   ├── (dashboard)/dashboard/  # Dashboard, parents, moderation, subscriptions, monitoring
│   ├── error.tsx               # Root error boundary
│   ├── layout.tsx
│   └── globals.css            # Brand theme (gold, maroon)
├── components/
│   ├── layout/                # Sidebar, header, dashboard guard
│   ├── ui/                    # shadcn: button, card, dialog, input, select, table, badge, skeleton
│   ├── empty-state.tsx
│   └── providers.tsx
├── contexts/
│   └── auth-context.tsx
├── hooks/
│   └── use-dashboard.ts       # Dashboard KPIs + charts (mock or API)
├── lib/
│   ├── api.ts                 # API client + JWT refresh
│   ├── mock-api.ts            # Mock data for dashboard, invoices, metrics, parents, stories
│   └── utils.ts
├── types/
│   └── api.ts                 # API + dashboard/invoice/monitoring types
└── styles/                    # (globals in app/globals.css)
```

See **DEPLOYMENT.md** for Docker and production deployment.
