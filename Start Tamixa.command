#!/bin/bash
# Double-click in Finder to start the whole Tamixa stack (DB, Redis, backend, web, admin).
cd "$(dirname "$0")" || exit 1
./scripts/run-all.sh
echo
echo "Services keep running after you close this window. Stop them with 'Stop Tamixa.command'."
