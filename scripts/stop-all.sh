#!/usr/bin/env bash
# Stop the backend, web and admin started by scripts/run-all.sh.
# PostgreSQL and Redis are left running (stop them with `brew services stop …` or `docker compose stop`).
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT/logs/run"

kill_tree() { # kill a PID and its children (gradle/npm spawn child processes)
  local pid="$1" child
  for child in $(pgrep -P "$pid" 2>/dev/null); do kill_tree "$child"; done
  kill "$pid" 2>/dev/null || true
}

for name in admin web backend; do
  f="$RUN_DIR/$name.pid"
  if [ -f "$f" ]; then
    pid="$(cat "$f")"
    if kill -0 "$pid" 2>/dev/null; then kill_tree "$pid"; echo "stopped $name (pid $pid)"; fi
    rm -f "$f"
  fi
done

# Anything still holding the ports that is a java/node dev process (e.g. started by hand).
# Never kill other owners (e.g. Docker Desktop's port proxy for the compose web/admin containers).
for port in 3001 3000 8080; do
  for pid in $(lsof -ti "tcp:$port" -sTCP:LISTEN 2>/dev/null || true); do
    cmd="$(ps -p "$pid" -o comm= 2>/dev/null | xargs basename 2>/dev/null)"
    case "$cmd" in
      java|node|next-server*|npm*) echo "freeing port $port ($cmd pid $pid)"; kill "$pid" 2>/dev/null || true ;;
      *) echo "port $port is held by '$cmd' (pid $pid) — not touching it. If it is Docker, run: docker compose stop web admin backend" ;;
    esac
  done
done
echo "done."
