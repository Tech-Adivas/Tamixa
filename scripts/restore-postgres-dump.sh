#!/usr/bin/env bash
# Restore PostgreSQL from a dump file into whichever database your env points at (same URL rules as check-db-connection.sh).
#
# --- Create staging tables + data from your local Tamixa DB (typical Railway staging) ---
#
# 1) On your machine, dump local (replace LOCAL_URL with how you connect to local Postgres, e.g. postgresql://postgres:...@localhost:5432/araro_kids).
#    Default pg_dump includes **schema and all row data** (omit --schema-only if you want rows on staging):
#      pg_dump -Fc --no-owner -f tamixa.local.dump "$LOCAL_URL"
#
# 2) Point env at **staging** Postgres only for this command (Railway: use the **public** URL from the Postgres
#    plugin so your laptop can connect — same as DATABASE_PUBLIC_URL on the backend service). Do not commit staging URLs.
#
# 3) Restore (drops objects that exist in the dump, then recreates them — use on empty or disposable staging DB):
#      CONFIRM_STAGING_RESTORE=1 RESTORE_CLEAN=1 ./scripts/restore-postgres-dump.sh ./tamixa.local.dump
#
# One-liner (dump + restore, **includes rows**): ./scripts/copy-local-postgres-to-staging.sh SOURCE_URL TARGET_URL
#
# If staging is empty, RESTORE_CLEAN=1 is usually what you want. If you only need schema and rely on Flyway instead,
# use ./scripts/run-flyway-migrate.sh against staging — no dump required.
#
# Supports:
#   - PostgreSQL custom format (.dump from pg_dump -Fc) → pg_restore
#   - Plain SQL → psql -f
#
# Usage (from repo root):
#   CONFIRM_STAGING_RESTORE=1 RESTORE_CLEAN=1 ./scripts/restore-postgres-dump.sh
#   ./scripts/restore-postgres-dump.sh /path/to/tamixa.local.dump /path/to/staging.env
#   ./scripts/restore-postgres-dump.sh /path/to.dump -   # “-” = do not load .env / railway.env (URLs must carry credentials)
#
# RESTORE_TARGET_POSTGRES_URL= — optional; if set, used as restore target instead of DATABASE_* from .env (see copy-local-postgres-to-staging.sh).
#
# RESTORE_CLEAN=1              — pg_restore --clean --if-exists (destructive; fixes “already exists” on repeat runs).
# RESTORE_JOBS=N               — parallel pg_restore (custom format), default 4.
# CONFIRM_STAGING_RESTORE=1  — required when the target URL looks like hosted cloud DB (e.g. *.rlwy.net); prevents accidents.
#
# Does not print passwords; target URL is logged with userinfo redacted.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DUMP="${1:-${TAMIXA_DUMP:-$ROOT/tamixa.local.dump}}"
# Relative paths are resolved from repo root (script always cd's there) so ./tamixa.local.dump works from any cwd.
if [[ "$DUMP" != /* ]]; then
  DUMP="${DUMP#./}"
  DUMP="$ROOT/$DUMP"
fi
ENV_FILE_OVERRIDE="${2:-}"

if [[ "$ENV_FILE_OVERRIDE" == "-" ]]; then
  ENV_FILE=""
elif [[ -n "$ENV_FILE_OVERRIDE" && -f "$ENV_FILE_OVERRIDE" ]]; then
  ENV_FILE="$ENV_FILE_OVERRIDE"
elif [[ -f "$ROOT/.env" ]]; then
  ENV_FILE="$ROOT/.env"
elif [[ -f "$ROOT/railway.env" ]]; then
  ENV_FILE="$ROOT/railway.env"
else
  ENV_FILE=""
fi

if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ ! -f "$DUMP" ]]; then
  echo "restore-postgres-dump: dump file not found (resolved path): $DUMP" >&2
  echo "  Create it from your local DB, e.g.:" >&2
  echo "    pg_dump -Fc --no-owner -f \"$ROOT/tamixa.local.dump\" \"postgresql://USER:PASS@127.0.0.1:5432/araro_kids\"" >&2
  echo "  Or copy local→staging in one step:" >&2
  echo "    CONFIRM_STAGING_RESTORE=1 RESTORE_CLEAN=1 ./scripts/copy-local-postgres-to-staging.sh 'postgresql://...local...' 'postgresql://...staging...'" >&2
  exit 1
fi

if [[ -z "${PGPASSWORD:-}" && -n "${POSTGRES_PASSWORD:-}" ]]; then
  export PGPASSWORD="$POSTGRES_PASSWORD"
fi

RAW="${RESTORE_TARGET_POSTGRES_URL:-}"
if [[ -z "$RAW" ]]; then
  RAW="${DATABASE_PUBLIC_URL:-${DATABASE_URL:-${SPRING_DATASOURCE_URL:-}}}"
fi

if [[ -z "$RAW" ]]; then
  echo "restore-postgres-dump: set RESTORE_TARGET_POSTGRES_URL or DATABASE_PUBLIC_URL / DATABASE_URL / SPRING_DATASOURCE_URL (e.g. via .env)." >&2
  exit 1
fi

if [[ "$RAW" =~ ^jdbc: ]]; then
  PSQL_URL="${RAW#jdbc:}"
else
  PSQL_URL="$RAW"
fi

if [[ ! "$PSQL_URL" =~ ^postgres(ql)?:// ]]; then
  echo "restore-postgres-dump: URL must be postgres:// or postgresql:// (or jdbc:postgresql://) after normalization." >&2
  exit 1
fi

REDACTED="$(printf '%s' "$PSQL_URL" | sed -E 's#(://)[^/@]+@#\1***@#')"
echo "restore-postgres-dump: target (redacted): $REDACTED"
echo "restore-postgres-dump: dump file: $DUMP"

# Require explicit opt-in before writing to obvious cloud / Railway hosts (avoid restoring onto staging by mistake).
if [[ "$PSQL_URL" =~ (proxy\.)?rlwy\.net ]] || [[ "$PSQL_URL" =~ \.rds\.amazonaws\.com ]]; then
  if [[ "${CONFIRM_STAGING_RESTORE:-}" != "1" ]]; then
    echo "restore-postgres-dump: target looks like a hosted database (e.g. Railway). To apply this dump there, run again with:" >&2
    echo "  CONFIRM_STAGING_RESTORE=1 RESTORE_CLEAN=1 ./scripts/restore-postgres-dump.sh ..." >&2
    exit 1
  fi
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "restore-postgres-dump: psql not found. Install PostgreSQL client (e.g. brew install libpq)." >&2
  exit 1
fi

MAGIC="$(head -c 5 "$DUMP" || true)"
if [[ "$MAGIC" == "PGDMP" ]]; then
  if ! command -v pg_restore >/dev/null 2>&1; then
    echo "restore-postgres-dump: pg_restore not found. Install PostgreSQL client (e.g. brew install libpq)." >&2
    exit 1
  fi
  JOBS="${RESTORE_JOBS:-4}"
  if [[ "$JOBS" =~ ^[0-9]+$ ]] && [[ "$JOBS" -gt 1 ]]; then
    JOB_ARG=(-j "$JOBS")
  else
    JOB_ARG=()
  fi
  RESTORE_ARGS=(--verbose --no-owner --no-acl "${JOB_ARG[@]}" -d "$PSQL_URL" "$DUMP")
  if [[ "${RESTORE_CLEAN:-}" == "1" ]]; then
    echo "restore-postgres-dump: RESTORE_CLEAN=1 → --clean --if-exists (drops existing objects in the dump)." >&2
    pg_restore --clean --if-exists "${RESTORE_ARGS[@]}"
  else
    echo "restore-postgres-dump: pg_restore without --clean. If you get 'already exists' errors, run again with RESTORE_CLEAN=1." >&2
    pg_restore "${RESTORE_ARGS[@]}"
  fi
else
  echo "restore-postgres-dump: treating as plain SQL (not PGDMP custom format)." >&2
  if [[ "${RESTORE_CLEAN:-}" == "1" ]]; then
    echo "restore-postgres-dump: RESTORE_CLEAN is ignored for plain SQL; the file must contain DROP statements if needed." >&2
  fi
  psql "$PSQL_URL" -v ON_ERROR_STOP=1 -f "$DUMP"
fi

echo "restore-postgres-dump: finished."
