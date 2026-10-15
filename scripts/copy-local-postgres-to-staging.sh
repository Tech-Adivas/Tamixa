#!/usr/bin/env bash
# Copy a **full** Postgres database (every table’s **schema + row data**) from a source URL to a target URL in one run.
# Uses pg_dump -Fc (default dump includes data; we do **not** pass --schema-only).
#
# Typical: local Tamixa → Railway staging (public postgres URL).
#
# Requirements: pg_dump, pg_restore, psql (e.g. brew install libpq).
#
# Usage (credentials in URLs or use libpq env vars your shell exports before running):
#   CONFIRM_STAGING_RESTORE=1 RESTORE_CLEAN=1 \
#     ./scripts/copy-local-postgres-to-staging.sh \
#     'postgresql://postgres:LOCAL@127.0.0.1:5432/araro_kids' \
#     'postgresql://postgres:STAGING@xxxx.proxy.rlwy.net:PORT/railway'
#
# Or:
#   export SOURCE_DATABASE_URL='postgresql://...local...'
#   export TARGET_DATABASE_URL='postgresql://...staging...'
#   CONFIRM_STAGING_RESTORE=1 RESTORE_CLEAN=1 ./scripts/copy-local-postgres-to-staging.sh
#
# RESTORE_CLEAN=1 is recommended for staging so existing objects from a previous partial run are replaced.
# This does not read repo .env for the restore step (avoids local PGPASSWORD confusing the staging connection).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SOURCE="${1:-${SOURCE_DATABASE_URL:-}}"
TARGET="${2:-${TARGET_DATABASE_URL:-}}"

if [[ -z "$SOURCE" || -z "$TARGET" ]]; then
  echo "copy-local-postgres-to-staging: need source and target connection URLs." >&2
  echo "  ./scripts/copy-local-postgres-to-staging.sh 'postgresql://...@localhost/db' 'postgresql://...@staging...'" >&2
  echo "  or set SOURCE_DATABASE_URL and TARGET_DATABASE_URL." >&2
  exit 1
fi

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "copy-local-postgres-to-staging: pg_dump not found (brew install libpq)." >&2
  exit 1
fi

TMP="$(mktemp "${TMPDIR:-/tmp}/tamixa-pg-copy.XXXXXX.dump")"
cleanup() { rm -f "$TMP"; }
trap cleanup EXIT

REDACT_S="$(printf '%s' "$SOURCE" | sed -E 's#(://)[^/@]+@#\1***@#')"
REDACT_T="$(printf '%s' "$TARGET" | sed -E 's#(://)[^/@]+@#\1***@#')"
echo "copy-local-postgres-to-staging: source (redacted): $REDACT_S"
echo "copy-local-postgres-to-staging: target (redacted): $REDACT_T"
echo "copy-local-postgres-to-staging: pg_dump -Fc (schema + **all rows**, not --schema-only) ..."

pg_dump -Fc --no-owner -f "$TMP" "$SOURCE"

export RESTORE_TARGET_POSTGRES_URL="$TARGET"
export RESTORE_CLEAN="${RESTORE_CLEAN:-}"
export CONFIRM_STAGING_RESTORE="${CONFIRM_STAGING_RESTORE:-}"

echo "copy-local-postgres-to-staging: restoring into target (via restore-postgres-dump.sh) ..."
# “-” skips loading .env so local DATABASE_* / PGPASSWORD do not override the staging target.
./scripts/restore-postgres-dump.sh "$TMP" "-"

echo "copy-local-postgres-to-staging: finished (schema + data on target). Restart staging API if needed."
