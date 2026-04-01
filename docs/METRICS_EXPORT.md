# PR metrics export (Sheets / BigQuery / webhooks)

This repo records optional AI SDLC fields in PR descriptions and exports structured JSON when a PR **merges** to `main` or `develop`.

## What runs in GitHub Actions

| Workflow | When | Purpose |
|----------|------|---------|
| [pr-governance-metrics.yml](../.github/workflows/pr-governance-metrics.yml) | PR open/sync | Artifact + job summary |
| [pr-metrics-export.yml](../.github/workflows/pr-metrics-export.yml) | PR **merged** | `pr-metrics-export.json` + optional **webhook POST** |
| [bootstrap-repo-labels.yml](../.github/workflows/bootstrap-repo-labels.yml) | Manual | Creates **`skip-spec-first`** label |

## 1. Create the `skip-spec-first` label (repository)

**Option A — workflow (recommended)**  
GitHub → **Actions** → **Bootstrap repo labels** → **Run workflow**.  
Uses `GITHUB_TOKEN` with `issues: write` (already set in the workflow).

**Option B — GitHub CLI**

```bash
gh label create "skip-spec-first" \
  --color FBCA04 \
  --description "Bypass spec-first-guard (maintainers only; use sparingly; follow up with spec/ADR)." \
  --repo OWNER/REPO
```

## 2. Organization-wide label (optional)

Org admins can define the same label for every repo (names and colors must be managed per repo on GitHub unless you use an org automation tool):

```bash
gh api --method POST "orgs/ORG_NAME/labels" \
  -f name='skip-spec-first' \
  -f color='FBCA04' \
  -f description='Bypass spec-first-guard (maintainers only).'
```

Repositories still need the label **present** on that repo for the bypass to apply; GitHub org default labels are not automatic for all existing repos—confirm your org’s settings.

## 3. Persistent metrics via webhook (Sheets or BigQuery)

### 3.1 Repository secret

Add **Settings → Secrets and variables → Actions → New repository secret**:

| Name | Value |
|------|--------|
| `PR_METRICS_WEBHOOK_URL` | HTTPS URL that accepts `POST` with `Content-Type: application/json` |
| `PR_METRICS_WEBHOOK_SECRET` (optional) | If set, sent as header `X-Metrics-Secret` for your endpoint to validate |

The workflow sends **`pr-metrics-export.json`** (built by [.github/workflows/pr-metrics-export.yml](../.github/workflows/pr-metrics-export.yml)) with this shape:

```json
{
  "pull_request": 123,
  "repository": "owner/repo",
  "ai_assisted": true,
  "eval_pass": false,
  "re_prompts": 2,
  "export": {
    "merged_at": "2026-03-24T12:00:00Z",
    "merge_sha": "abc…",
    "base_ref": "main",
    "head_ref": "feature/foo",
    "pr_title": "…",
    "pr_url": "https://github.com/…",
    "merged_by": "login",
    "workflow_run_url": "https://github.com/…/actions/runs/…"
  }
}
```

The POST step uses **`continue-on-error: true`** so a bad endpoint does not block merges.

### 3.2 Google Sheets (Apps Script web app)

1. Create a Sheet with headers, e.g.  
   `merged_at, repository, pull_request, ai_assisted, eval_pass, re_prompts, merge_sha, base_ref, head_ref, pr_title, pr_url, merged_by`
2. **Extensions → Apps Script** — deploy as **Web app** (POST, execute as you, accessible to anyone with URL — **use a secret path + validate** or lock down via secret header).
3. Example handler (adapt sheet name and column order):

```javascript
function doPost(e) {
  const body = JSON.parse(e.postData.contents);
  const exp = body.export || {};
  const row = [
    exp.merged_at,
    body.repository,
    body.pull_request,
    body.ai_assisted,
    body.eval_pass,
    body.re_prompts,
    exp.merge_sha,
    exp.base_ref,
    exp.head_ref,
    exp.pr_title,
    exp.pr_url,
    exp.merged_by,
  ];
  SpreadsheetApp.getActiveSpreadsheet().getSheetByName('pr_metrics').appendRow(row);
  return ContentService.createTextOutput('ok');
}
```

4. Put the web app URL in **`PR_METRICS_WEBHOOK_URL`**.

**Hardening:** set repository secret **`PR_METRICS_WEBHOOK_SECRET`**; the workflow sends **`X-Metrics-Secret`** automatically when this secret is defined. In Apps Script, compare `e.parameter` / headers to Script Properties before appending rows.

### 3.3 BigQuery (recommended pattern)

Do **not** put service account JSON in the workflow for simple setups. Instead:

1. Deploy a **Cloud Run / Cloud Functions (2nd gen)** HTTPS endpoint that:
   - Validates a shared secret header or OIDC from GitHub (advanced).
   - Inserts one row into a BigQuery table via the **BigQuery Storage Write API** or `INSERT` DML.
2. Set **`PR_METRICS_WEBHOOK_URL`** to that URL.

Table schema example:

| column | type |
|--------|------|
| merged_at | TIMESTAMP |
| repository | STRING |
| pull_request | INT64 |
| ai_assisted | BOOL |
| eval_pass | BOOL |
| re_prompts | INT64 |
| merge_sha | STRING |
| raw_json | STRING (full payload for evolution) |

## 4. Related docs

- [specs/README.md](specs/README.md) — spec-first CI and `skip-spec-first`  
- [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md) — metrics semantics  
