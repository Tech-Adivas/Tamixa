# Revenue Analytics Dashboard

## Overview

The Revenue Analytics dashboard provides SaaS metrics, revenue charts, real-time alerts, and a detailed revenue table for the Tamixa Admin Panel.

## Folder Structure

```
admin/
├── src/
│   ├── app/(dashboard)/dashboard/
│   │   └── revenue/
│   │       ├── page.tsx          # Main revenue analytics page
│   │       └── loading.tsx        # Loading skeleton
│   ├── components/
│   │   ├── charts/
│   │   │   ├── index.ts
│   │   │   ├── mrr-line-chart.tsx
│   │   │   ├── plan-pie-chart.tsx
│   │   │   ├── churn-bar-chart.tsx
│   │   │   └── story-generation-bar-chart.tsx
│   │   └── revenue/
│   │       └── revenue-alerts-banner.tsx
│   ├── lib/
│   │   ├── api.ts                # API client (extended with revenue endpoints)
│   │   └── revenue-api.ts        # Revenue API service + mock fallback
│   └── types/
│       └── api.ts                # Revenue types
```

## Features

### 1. Dashboard Metrics Cards
- **MRR** – Monthly recurring revenue
- **Active subscriptions** – Current paid subscribers
- **Trial conversion rate**
- **Churn rate**
- **Free limit hits**
- **AI token cost (monthly)**

### 2. Revenue Charts
- **MRR over time** – Line chart (Recharts)
- **Plan distribution** – Pie chart
- **Churn trend** – Bar chart
- **Story generation per plan** – Bar chart

### 3. API Integration
- `GET /api/v1/admin/metrics/revenue` – Revenue for a given month
- `GET /api/v1/admin/metrics/subscription` – Aggregated subscription metrics
- `GET /api/v1/admin/metrics/ai` – AI usage (tokens, cache)
- `GET /api/v1/admin/metrics/revenue/table` – Paginated revenue table
- Loading states on all data fetches

### 4. Real-Time Alerts
Warning banner when:
- Payment failure rate > 5%
- AI token usage spike detected
- Churn rate increases above 8%

### 5. Revenue Table
Columns: Parent ID, Plan, Subscription state, Monthly payment, Created date  
- Search by parent ID or email  
- Filter by plan (Free, Monthly, Yearly, Family, Voice Premium)  
- Pagination

### 6. Security
- **Admin role validation** – `DashboardGuard` checks `user.role === "ADMIN"`
- **JWT middleware** – `fetchWithAuth` adds `Authorization: Bearer <token>`, refreshes on 401
- **Route protection** – All `/dashboard/*` routes wrapped by `DashboardGuard`
- Backend `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints

## Usage

1. **Mock mode** – Set `NEXT_PUBLIC_USE_MOCK_API=true` for development without the backend.
2. **Production** – Configure `NEXT_PUBLIC_API_URL` to your backend (e.g. `https://api.tamixa.com`).

## Dark Mode
The dashboard supports dark mode via `next-themes` (header toggle).
