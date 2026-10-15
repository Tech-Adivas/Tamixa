#!/usr/bin/env bash
# Run Flyway migrations against the database URL used by the Spring Boot app (same env resolution as staging).
# Railway public URLs (*.proxy.rlwy.net / *.rlwy.net) get sslmode=require in Gradle — same rule as TamixaApplication.
#
# If `.env` contains any non-empty RAILWAY_* variable (often copied from Railway), Gradle prefers DATABASE_URL first,
# same as the running app on Railway. From a laptop, DATABASE_URL may be a private host (postgres.railway.internal) and
# will not connect — unset RAILWAY_* for this script, or set DATABASE_URL to the public proxy URL for a one-off migrate.
#
# Use when app nodes run with FLYWAY_ENABLED=false (staging/prod) and the DB is behind the deployed JAR
# (e.g. Schema-validation: missing table [admin_audit]).
#
# Usage (from repo root):
#   export DATABASE_URL='postgresql://...'   # or DATABASE_PUBLIC_URL
#   export DATABASE_USERNAME=... DATABASE_PASSWORD=...
#   ./scripts/run-flyway-migrate.sh
#
# Or source `.env` / `railway.env` the same way as check-db-connection.sh.
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

if [[ -z "${DATABASE_PUBLIC_URL:-}" && -z "${DATABASE_URL:-}" ]]; then
  echo "run-flyway-migrate: set DATABASE_PUBLIC_URL or DATABASE_URL (or source .env / railway.env)." >&2
  exit 1
fi

if [[ -n "${DATABASE_PUBLIC_URL:-}" && -n "${DATABASE_URL:-}" ]]; then
  echo "run-flyway-migrate: both DATABASE_PUBLIC_URL and DATABASE_URL are set." >&2
  echo "  Gradle picks one order based on RAILWAY_* (see :backend:flywayShowTarget). If staging still has no tables," >&2
  echo "  the backend service on Railway may be using the *other* URL — point both at the same Postgres (variable reference)." >&2
fi

echo "run-flyway-migrate: target (verify host/db match Railway backend Postgres):"
./gradlew :backend:flywayShowTarget --no-daemon -q

echo "run-flyway-migrate: applying migrations via :backend:flywayMigrate (OS env + .env merged in Gradle)."
./gradlew :backend:flywayMigrate --no-daemon

echo "run-flyway-migrate: migration status:"
./gradlew :backend:flywayInfo --no-daemon || true

echo "run-flyway-migrate: done. Redeploy or restart app nodes if they were crash-looping on schema validation."
