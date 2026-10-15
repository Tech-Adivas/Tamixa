#!/usr/bin/env bash
# Verify Postgres connectivity using the same env vars the Spring Boot app uses on Railway.
#
# Usage:
#   ./scripts/check-db-connection.sh
#   # Loads repo-root `.env` if present, else `railway.env` (Railway mirror — gitignored).
#   ./scripts/check-db-connection.sh /path/to/custom.env
#
# Resolution order: DATABASE_PUBLIC_URL → DATABASE_URL → SPRING_DATASOURCE_URL
# jdbc:postgresql://... is normalized to postgresql://... for psql.
# If the URL has no embedded password, ensure PGPASSWORD or POSTGRES_PASSWORD is set (psql reads PGPASSWORD).
#
# Does not print secrets: connection target is logged with userinfo redacted.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -n "${1:-}" ]]; then
  ENV_FILE="$1"
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

# Align with app: POSTGRES_PASSWORD is common on Railway; psql uses PGPASSWORD.
if [[ -z "${PGPASSWORD:-}" && -n "${POSTGRES_PASSWORD:-}" ]]; then
  export PGPASSWORD="$POSTGRES_PASSWORD"
fi

RAW="${DATABASE_PUBLIC_URL:-${DATABASE_URL:-${SPRING_DATASOURCE_URL:-}}}"

if [[ -z "$RAW" ]]; then
  echo "check-db-connection: no DATABASE_PUBLIC_URL, DATABASE_URL, or SPRING_DATASOURCE_URL set." >&2
  echo "  Tip: copy railway.env.example → railway.env, or merge Railway vars into .env, or export vars in this shell." >&2
  exit 1
fi

if [[ "$RAW" =~ ^jdbc: ]]; then
  PSQL_URL="${RAW#jdbc:}"
else
  PSQL_URL="$RAW"
fi

if [[ ! "$PSQL_URL" =~ ^postgres(ql)?:// ]]; then
  echo "check-db-connection: after normalization, URL must start with postgresql:// or postgres:// (got scheme other than postgres)." >&2
  exit 1
fi

REDACTED="$(printf '%s' "$PSQL_URL" | sed -E 's#(://)[^/@]+@#\1***@#')"
echo "check-db-connection: testing (redacted): $REDACTED"

if ! command -v psql >/dev/null 2>&1; then
  echo "check-db-connection: psql not found. Install PostgreSQL client (e.g. brew install libpq)." >&2
  exit 1
fi

if psql "$PSQL_URL" -v ON_ERROR_STOP=1 -c 'SELECT 1 AS connection_ok;' >/dev/null; then
  echo "check-db-connection: OK (SELECT 1 succeeded)."
  exit 0
fi

exit 1
