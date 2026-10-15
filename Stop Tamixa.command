#!/bin/bash
# Double-click in Finder to stop backend, web and admin (Postgres/Redis keep running).
cd "$(dirname "$0")" || exit 1
./scripts/stop-all.sh
