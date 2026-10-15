#!/usr/bin/env bash
# Forward to Docker Compose on CI and local machines.
# GitHub Actions ubuntu-latest provides Compose V2 as `docker compose` only;
# older setups may still have the `docker-compose` binary.
set -euo pipefail

if docker compose version >/dev/null 2>&1; then
  exec docker compose "$@"
fi
if command -v docker-compose >/dev/null 2>&1; then
  exec docker-compose "$@"
fi
echo "::error::Docker Compose not found (need 'docker compose' or docker-compose on PATH)." >&2
exit 1
