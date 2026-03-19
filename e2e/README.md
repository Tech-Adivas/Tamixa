# Tamixa E2E Tests

Playwright-based end-to-end and smoke tests for the web app and admin dashboard.

## Prerequisites

- Node.js 18+
- Backend running at `http://localhost:8080` (for full flows)
- Web app running at `http://localhost:3000` (default)
- Admin app running at `http://localhost:3001` (default)

## Setup

```bash
cd e2e
npm install
npx playwright install chromium
```

## Run tests

```bash
# All E2E tests
npm run test

# Smoke tests only (fast)
npm run test:smoke

# Web app tests only
npm run test:web

# Admin tests only
npm run test:admin
```

## Environment

| Variable         | Default              | Description        |
|------------------|----------------------|--------------------|
| `WEB_BASE_URL`   | `http://localhost:3000` | Web app base URL |
| `ADMIN_BASE_URL` | `http://localhost:3001` | Admin app base URL |

Example with custom URLs:

```bash
WEB_BASE_URL=http://localhost:5173 ADMIN_BASE_URL=http://localhost:3002 npm run test:smoke
```

## Smoke flow checklist (manual)

For full release verification, run through:

1. **Web**: Register → Login → Dashboard → Children (add child) → Stories (generate/play) → Subscription → Settings → Logout
2. **Admin**: Login (ADMIN user) → Dashboard → Parents list → Story moderation → Curated stories → Subscriptions
3. **Backend**: `curl -s http://localhost:8080/actuator/health | jq .status` → `"UP"`
