#!/usr/bin/env bash
# Build only the backend (skips mobile which requires Android SDK).
# Usage: ./scripts/build-backend.sh
# To run: ./gradlew :backend:bootRun -Ptamixa.backendOnly=true
set -e
cd "$(dirname "$0")/.."
./gradlew :backend:build -Ptamixa.backendOnly=true --no-daemon -x test
