# Admin Implementation Gap Analysis

Analysis of the Tamixa admin dashboard against the spec in `DASHBOARD_VISUALIZATION.md`, backend capabilities, and implementation quality.

---

## 1. Navigation & Discoverability

| Spec Page | Route | In Sidebar? | Status |
|-----------|-------|-------------|--------|
| Dashboard | `/dashboard` | ✅ Yes | Implemented |
| Revenue Analytics | `/dashboard/revenue` | ✅ Yes | Implemented |
| Parent management | `/dashboard/parents` | ✅ Yes | Implemented |
| Story moderation | `/dashboard/moderation` | ✅ Yes | Implemented |
| Curated stories | `/dashboard/curated-stories` | ✅ Yes | Implemented |
| Subscriptions | `/dashboard/subscriptions` | ✅ Yes | Implemented |
| System monitoring | `/dashboard/monitoring` | ✅ Yes | Implemented |
| **Stories** | `/dashboard/stories` | ❌ No | Page exists, not in nav |
| **Children** | `/dashboard/children` | ❌ No | Page exists, not in nav |
| **Voice logs** | `/dashboard/voice-logs` | ❌ No | Page exists, not in nav |
| **Health** | `/dashboard/health` | ❌ No | Page exists, not in nav |
| **AI metrics** | `/dashboard/ai-metrics` | ❌ No | Page exists, not in nav |
| **Kafka** | `/dashboard/kafka` | ❌ No | Stub page, not in nav |
| **Audit** | `/dashboard/audit` | ❌ No | Stub page, not in nav |

**Gap:** Seven pages exist but are not linked in the sidebar. Users can only reach them via direct URL.

---

## 2. Backend API Integration Gaps

### Dashboard Overview
- **Current:** `use-dashboard.ts` **always** uses mock data. It never calls the real API.
- **Backend:** Has `/metrics/revenue`, `/metrics/subscription`, `/metrics/ai`, but no single dashboard KPI endpoint.
- **Gap:** Dashboard KPIs, revenue chart, and story usage chart are never wired to live backend data.

### Parent Management
- **Search:** UI says "Search by name or email", but backend only supports `email` search (`findByEmailContainingIgnoreCase`). No name search.
- **Suspend:** Backend has **no** suspend endpoint. Admin `handleSuspend` uses mock (`setTimeout`). Suspend is a no-op against real API.
- **ParentSummaryDto:** Backend returns `id`, `email`, `role`, `createdAt` only. Admin assumes `name`, `plan`, `status` and derives/fakes them client-side.

### Story Moderation
- **Approve / Reject:** Backend has **no** approve or reject endpoints. Both actions use mock `setTimeout`. Real moderation only supports **Flag**.
- **Flag bug:** In `moderation/page.tsx`, `handleFlag` checks `if (res.ok)` but `api.admin.flagStory` returns `Promise<void>`. On success, `res` is `undefined`, so `res.ok` throws `TypeError`. User always sees "Failed to flag story" even when the backend flag succeeds.
- **Story preview:** Full story content is not loaded from the API. Mock provides content; real API path shows placeholder only.

### Subscriptions
- **Invoices:** Backend has **no** invoices endpoint. Invoices are always from `mockApi.getMockInvoices()`.
- **Refund:** Mock only. No backend integration.

### System Monitoring
- **Current:** Partially integrated (health + AI metrics), but latency, Kafka lag, Redis, error rate, AI cost are mostly mock or hardcoded.
- **Backend:** Has health and AI metrics. No Kafka lag, Redis hit ratio, or error-rate APIs.

---

## 3. Feature Completeness vs Spec

| Feature | Spec | Implemented | Notes |
|---------|------|-------------|-------|
| Dashboard KPIs | Yes | Mock only | Never calls backend |
| Revenue chart | Yes | Mock only | |
| Story usage chart | Yes | Mock only | |
| Parent table | Yes | ✅ API | Email search only |
| Parent suspend | Yes | Mock | No backend endpoint |
| Story moderation (Flag) | Yes | ✅ API | Bug in success handling |
| Story moderation (Approve) | Yes | Mock | No backend endpoint |
| Story moderation (Reject) | Yes | Mock | No backend endpoint |
| Curated stories CRUD | Yes | ✅ API | Create, list, bulk actions |
| Subscription table | Yes | ✅ API | |
| Invoice table | Yes | Mock | No backend |
| Refund | Yes | Mock | No backend |
| System monitoring | Yes | Partial | Some mock metrics |
| Kafka events | Stub | Stub | Empty list from backend |
| Audit trail | Stub | Stub | Empty list from backend |

---

## 4. Code Quality & Consistency

### ESLint / Build
- `curated-stories/page.tsx`: Unused imports (`CreateCuratedStoryRequest`, `Input`, `Label`, `Dialog`, `DialogContent`, etc.). Build fails.
- `curated-stories/new/page.tsx`: Uses `<img>` instead of `next/image` (warning).

### Error Handling
- Many pages swallow errors or fall back to mock without clear user feedback when the backend is unreachable.
- `use-dashboard` ignores real API entirely; always uses mock.

### Children Page
- Filter by Parent ID requires form submit. Could support live filter or debounced search for better UX.
- No empty-state component when `data.content.length === 0`.

### Voice Logs
- Basic table; no special handling or empty state beyond the table.

---

## 5. Backend vs Admin Mismatches

| Admin Expectation | Backend Reality |
|-------------------|-----------------|
| Parent `name` | Not returned (admin derives from email) |
| Parent `plan`, `status` | Not in ParentSummaryDto (admin fakes) |
| Parent suspend | No endpoint |
| Story approve/reject | No endpoints |
| Story flag → status change | `flagStory` only logs to audit; does not update story status |
| Invoices | No endpoint |
| Refund | No endpoint |
| Dashboard KPIs | No unified endpoint; metrics scattered |

---

## 6. Recommendations

### High Priority
1. **Fix moderation flag bug:** Change `handleFlag` to treat success when `flagStory` completes without throwing (remove `res.ok` check).
2. **Fix curated-stories ESLint errors:** Remove unused imports so the build passes.
3. **Add missing nav links:** Add Stories, Children, Voice logs, Health, AI metrics to the sidebar (or a "More" submenu).

### Medium Priority
4. **Wire dashboard to backend:** Use `getRevenueMetrics`, `getSubscriptionMetrics`, `getAiMetrics` in `use-dashboard` when mock is disabled.
5. **Parent search label:** Change to "Search by email" to match backend.
6. **Story preview:** Fetch full story content from backend when available.

### Implemented (Backend + Admin)
7. **Parent suspend:** Backend `POST /admin/parents/{id}/suspend`, auth blocks suspended accounts, admin wired.
8. **Story approve/reject:** Backend `POST /admin/stories/{id}/approve`, `POST /admin/stories/{id}/reject`, admin wired.
9. **Invoices & Refund:** Backend `GET /admin/invoices`, `POST /admin/invoices/{id}/refund`, admin wired.
10. **Flag updates status:** `flagStory` now sets story status to FLAGGED in the DB.
11. **Story usage per day:** Backend `GET /admin/metrics/story-usage?days=7`, dashboard wired.
12. **Moderation flags count:** FLAGGED status added to StoryStatus, dashboard uses real count.
