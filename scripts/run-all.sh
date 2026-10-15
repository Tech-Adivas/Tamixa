#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Tamixa — start the whole local stack on macOS / Linux with one command.
#
#   ./scripts/run-all.sh          start DB + Redis + backend + web + admin
#   ./scripts/stop-all.sh         stop backend + web + admin (DB/Redis keep running)
#
# What it does (idempotent — anything already running is left alone):
#   1. PostgreSQL :5432 and Redis :6379  (Homebrew services → Docker Desktop fallback)
#   2. Backend   :8080  ./gradlew :backend:bootRun  (reads repo-root .env, dev profile)
#      then seeds the dev admin  admin@techadivas.com / Admin123!
#   3. Web       :3000  (Vite)       npm install when package.json changed
#   4. Admin     :3001  (Next.js)    creates admin/.env.local if missing
# Logs + PIDs: logs/run/*.log, logs/run/*.pid
# Written for macOS default bash 3.2 (no bash-4 features).
# ---------------------------------------------------------------------------
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT/logs/run"
mkdir -p "$RUN_DIR"
cd "$ROOT" || exit 1

BACKEND_PORT="${SERVER_PORT:-8080}"
WEB_PORT=3000
ADMIN_PORT=3001
API="http://127.0.0.1:${BACKEND_PORT}"

c_ok()   { printf '\033[32m✔ %s\033[0m\n' "$*"; }
c_info() { printf '\033[36m➜ %s\033[0m\n' "$*"; }
c_warn() { printf '\033[33m! %s\033[0m\n' "$*"; }
c_err()  { printf '\033[31m✘ %s\033[0m\n' "$*"; }

port_open() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && exec 3>&- 3<&- ; }
http_ok()   { curl -fsS -m 3 "$1" >/dev/null 2>&1; }

wait_for() { # wait_for <description> <seconds> <command...>
  local desc="$1" secs="$2"; shift 2
  local i=0
  while [ "$i" -lt "$secs" ]; do
    if "$@"; then return 0; fi
    i=$((i + 2)); sleep 2
    if [ $((i % 20)) -eq 0 ]; then c_info "…still waiting for $desc (${i}s)"; fi
  done
  return 1
}

# Read one KEY from repo-root .env (last occurrence wins, like the backend).
env_value() {
  [ -f "$ROOT/.env" ] || return 0
  grep -E "^[[:space:]]*$1=" "$ROOT/.env" | tail -1 | sed -E "s/^[[:space:]]*$1=//; s/^['\"]//; s/['\"]$//"
}

# ---------------------------------------------------------------------------
# 0. Tooling
# ---------------------------------------------------------------------------
echo; c_info "Tamixa local stack — $ROOT"

if [ ! -f "$ROOT/.env" ]; then
  c_warn ".env not found — copying .env.example (edit it later for real API keys)"
  cp "$ROOT/.env.example" "$ROOT/.env"
fi

# Java 17+ for Gradle / Spring Boot
java_major() { "$1" -version 2>&1 | grep -m1 -E 'version "' | sed -E 's/.*version "([0-9]+).*/\1/'; }
if [ -z "${JAVA_HOME:-}" ] || [ "$(java_major "$JAVA_HOME/bin/java" 2>/dev/null || echo 0)" -lt 17 ] 2>/dev/null; then
  if [ -x /usr/libexec/java_home ] && /usr/libexec/java_home -v 17+ >/dev/null 2>&1; then
    JAVA_HOME="$(/usr/libexec/java_home -v 17+)"
  elif [ -d "$HOME/.sdkman/candidates/java/current" ]; then
    JAVA_HOME="$HOME/.sdkman/candidates/java/current"
  fi
  export JAVA_HOME
fi
if [ -z "${JAVA_HOME:-}" ] || [ "$(java_major "$JAVA_HOME/bin/java" 2>/dev/null || echo 0)" -lt 17 ] 2>/dev/null; then
  c_err "JDK 17+ not found. Install: brew install --cask temurin@17  (or sdk install java 17-tem)"; exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
c_ok "Java $(java_major "$JAVA_HOME/bin/java") at $JAVA_HOME"

# Node 20.19+ (Vite 7 / Next 15). Use nvm if the current node is too old.
node_ok() {
  command -v node >/dev/null 2>&1 || return 1
  local v maj min; v="$(node -v | sed 's/^v//')"; maj="${v%%.*}"; min="$(echo "$v" | cut -d. -f2)"
  [ "$maj" -gt 20 ] || { [ "$maj" -eq 20 ] && [ "$min" -ge 19 ]; }
}
if ! node_ok && [ -s "$HOME/.nvm/nvm.sh" ]; then
  # shellcheck disable=SC1091
  . "$HOME/.nvm/nvm.sh" >/dev/null 2>&1
  nvm use 22 >/dev/null 2>&1 || nvm use 20 >/dev/null 2>&1 || nvm install 22 >/dev/null 2>&1
fi
if ! node_ok; then
  c_err "Node.js 20.19+ required (found: $(node -v 2>/dev/null || echo none)). Install: brew install node@22  or  nvm install 22"; exit 1
fi
c_ok "Node $(node -v)"

# ---------------------------------------------------------------------------
# 1. PostgreSQL + Redis
# ---------------------------------------------------------------------------
docker_ready() { command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; }
start_docker_desktop() {
  docker_ready && return 0
  if [ -d "/Applications/Docker.app" ] || [ -d "$HOME/Applications/Docker.app" ]; then
    c_info "Starting Docker Desktop…"; open -a Docker >/dev/null 2>&1 || true
    wait_for "Docker Desktop" 120 docker_ready
    return $?
  fi
  return 1
}
brew_start() { # brew_start <formula-prefix>
  command -v brew >/dev/null 2>&1 || return 1
  local f; f="$(brew services list 2>/dev/null | awk '{print $1}' | grep -E "^$1(@[0-9]+)?$" | sort | tail -1)"
  [ -n "$f" ] || f="$(brew list --formula 2>/dev/null | grep -E "^$1(@[0-9]+)?$" | sort | tail -1)"
  [ -n "$f" ] || return 1
  c_info "brew services start $f"; brew services start "$f" >/dev/null 2>&1
}

if port_open 5432; then
  c_ok "PostgreSQL already running on :5432"
else
  if brew_start postgresql && wait_for "PostgreSQL" 30 port_open 5432; then
    c_ok "PostgreSQL started (Homebrew)"
  elif start_docker_desktop && docker compose up -d postgres >/dev/null && wait_for "PostgreSQL" 60 port_open 5432; then
    c_ok "PostgreSQL started (Docker)"
  else
    c_err "Could not start PostgreSQL. Install one: brew install postgresql@16 && brew services start postgresql@16 — or install Docker Desktop."; exit 1
  fi
fi

if port_open 6379; then
  c_ok "Redis already running on :6379"
else
  if brew_start redis && wait_for "Redis" 20 port_open 6379; then
    c_ok "Redis started (Homebrew)"
  elif command -v redis-server >/dev/null 2>&1 && redis-server --daemonize yes >/dev/null && wait_for "Redis" 10 port_open 6379; then
    c_ok "Redis started (redis-server)"
  elif start_docker_desktop && docker compose up -d redis >/dev/null && wait_for "Redis" 40 port_open 6379; then
    c_ok "Redis started (Docker)"
  else
    c_err "Could not start Redis. Install: brew install redis && brew services start redis"; exit 1
  fi
fi

# Make sure the database named in .env exists (only when psql is available and DB is local).
DB_URL="$(env_value DATABASE_URL)"
DB_NAME="$(echo "${DB_URL:-jdbc:postgresql://localhost:5432/araro_kids}" | sed -E 's#.*/([^/?]+)(\?.*)?$#\1#')"
DB_USER="$(env_value DATABASE_USERNAME)"; DB_USER="${DB_USER:-$(env_value POSTGRES_USER)}"; DB_USER="${DB_USER:-postgres}"
DB_PASS="$(env_value DATABASE_PASSWORD)"; DB_PASS="${DB_PASS:-$(env_value POSTGRES_PASSWORD)}"; DB_PASS="${DB_PASS:-postgres}"
if command -v psql >/dev/null 2>&1 && echo "${DB_URL:-localhost}" | grep -qE "localhost|127\.0\.0\.1"; then
  if PGPASSWORD="$DB_PASS" psql -h 127.0.0.1 -U "$DB_USER" -d postgres -tAc "select 1" >/dev/null 2>&1; then
    if [ "$(PGPASSWORD="$DB_PASS" psql -h 127.0.0.1 -U "$DB_USER" -d postgres -tAc "select 1 from pg_database where datname='$DB_NAME'")" != "1" ]; then
      PGPASSWORD="$DB_PASS" psql -h 127.0.0.1 -U "$DB_USER" -d postgres -c "create database \"$DB_NAME\"" >/dev/null && c_ok "Created database $DB_NAME"
    else
      c_ok "Database $DB_NAME exists"
    fi
  else
    c_warn "Cannot log in to Postgres as '$DB_USER' with the password in .env — check DATABASE_USERNAME / DATABASE_PASSWORD"
  fi
fi

# ---------------------------------------------------------------------------
# 2. Backend
# ---------------------------------------------------------------------------
if http_ok "$API/actuator/health"; then
  c_ok "Backend already running → $API"
else
  c_info "Starting backend (first run downloads dependencies — can take 5–10 min)…"
  chmod +x ./gradlew 2>/dev/null || true
  SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}" \
    nohup ./gradlew :backend:bootRun -Ptamixa.backendOnly=true > "$RUN_DIR/backend.log" 2>&1 &
  echo $! > "$RUN_DIR/backend.pid"
  if wait_for "backend health ($API/actuator/health)" 600 http_ok "$API/actuator/health"; then
    c_ok "Backend up → $API   (log: logs/run/backend.log)"
  else
    c_err "Backend did not become healthy. Last log lines:"; tail -40 "$RUN_DIR/backend.log"; exit 1
  fi
fi
# Dev-only admin seed (404/403 outside the dev profile is fine)
curl -fsS -m 10 -X POST "$API/api/v1/dev/seed-admin" >/dev/null 2>&1 && c_ok "Dev admin ready: admin@techadivas.com / Admin123!"

# ---------------------------------------------------------------------------
# 3 + 4. Web and Admin
# ---------------------------------------------------------------------------
ensure_deps() { # ensure_deps <dir>
  local d="$1"
  if [ ! -d "$d/node_modules" ] || [ ! -f "$d/node_modules/.package-lock.json" ] \
     || [ "$d/package.json" -nt "$d/node_modules/.package-lock.json" ] \
     || [ "$d/package-lock.json" -nt "$d/node_modules/.package-lock.json" ]; then
    c_info "npm install in $d (dependencies changed)…"
    (cd "$d" && npm install --no-audit --no-fund > "$RUN_DIR/$(basename "$d")-npm.log" 2>&1) \
      || { c_err "npm install failed in $d — see logs/run/$(basename "$d")-npm.log"; return 1; }
  fi
}

if [ ! -f "$ROOT/admin/.env.local" ]; then
  printf 'API_URL=%s\nNEXT_PUBLIC_API_URL=%s\n' "$API" "$API" > "$ROOT/admin/.env.local"
  c_ok "Created admin/.env.local (API_URL=$API)"
fi

if http_ok "http://127.0.0.1:$WEB_PORT/"; then
  c_ok "Web already running → http://localhost:$WEB_PORT"
else
  ensure_deps "$ROOT/web" || exit 1
  (cd "$ROOT/web" && VITE_API_BASE_URL="$API" nohup npm run dev -- --port "$WEB_PORT" > "$RUN_DIR/web.log" 2>&1 & echo $! > "$RUN_DIR/web.pid")
  wait_for "web" 90 http_ok "http://127.0.0.1:$WEB_PORT/" && c_ok "Web up → http://localhost:$WEB_PORT" \
    || { c_err "Web did not start — see logs/run/web.log"; tail -20 "$RUN_DIR/web.log"; }
fi

if http_ok "http://127.0.0.1:$ADMIN_PORT/login"; then
  c_ok "Admin already running → http://localhost:$ADMIN_PORT"
else
  ensure_deps "$ROOT/admin" || exit 1
  (cd "$ROOT/admin" && API_URL="$API" NEXT_PUBLIC_API_URL="$API" nohup npm run dev > "$RUN_DIR/admin.log" 2>&1 & echo $! > "$RUN_DIR/admin.pid")
  wait_for "admin" 180 http_ok "http://127.0.0.1:$ADMIN_PORT/login" && c_ok "Admin up → http://localhost:$ADMIN_PORT" \
    || { c_err "Admin did not start — see logs/run/admin.log"; tail -20 "$RUN_DIR/admin.log"; }
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo
echo "────────────────────────────────────────────────────────────"
echo " Backend  $API           health: $API/actuator/health"
echo " Swagger  $API/swagger-ui.html"
echo " Web      http://localhost:$WEB_PORT     login: any email + code 123456"
echo " Admin    http://localhost:$ADMIN_PORT     admin@techadivas.com / Admin123!"
echo " Mobile   emulator uses http://10.0.2.2:$BACKEND_PORT (see docs/BEGINNERS_GUIDE.md §8)"
echo " Logs     logs/run/*.log      Stop: ./scripts/stop-all.sh"
echo "────────────────────────────────────────────────────────────"
if command -v open >/dev/null 2>&1 && [ "${NO_BROWSER:-0}" != "1" ]; then
  open "http://localhost:$ADMIN_PORT/login" >/dev/null 2>&1 || true
fi
