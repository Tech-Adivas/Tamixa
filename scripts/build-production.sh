#!/usr/bin/env bash
# Build all production artifacts for Tamixa deployment.
# Usage: ./scripts/build-production.sh [--backend-only | --web-only | --admin-only | --mobile-only]
# Default: builds backend (Docker), web, admin, mobile release.
# Set API_URL for web/admin builds: API_URL=https://api.tamixa.in
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API_URL="${API_URL:-https://api.tamixa.in}"
BUILD_ALL=true
BUILD_BACKEND=false
BUILD_WEB=false
BUILD_ADMIN=false
BUILD_MOBILE=false

for arg in "$@"; do
  case "$arg" in
    --backend-only) BUILD_ALL=false; BUILD_BACKEND=true ;;
    --web-only)      BUILD_ALL=false; BUILD_WEB=true ;;
    --admin-only)    BUILD_ALL=false; BUILD_ADMIN=true ;;
    --mobile-only)   BUILD_ALL=false; BUILD_MOBILE=true ;;
  esac
done

if $BUILD_ALL; then
  BUILD_BACKEND=true
  BUILD_WEB=true
  BUILD_ADMIN=true
  BUILD_MOBILE=true
fi

echo "== Tamixa Production Build =="
echo "API_URL=$API_URL"
echo ""

if $BUILD_BACKEND; then
  echo ">>> Building backend Docker image..."
  cd "$ROOT"
  docker build -t tamixa-backend:prod .
  echo ">>> Backend: tamixa-backend:prod"
  echo ""
fi

if $BUILD_WEB; then
  echo ">>> Building web..."
  cd "$ROOT/web"
  npm run build
  echo ">>> Web: web/dist/"
  echo ""
fi

if $BUILD_ADMIN; then
  echo ">>> Building admin..."
  cd "$ROOT/admin"
  NEXT_PUBLIC_API_URL="$API_URL" NEXT_PUBLIC_USE_MOCK_API=false npm run build
  echo ">>> Admin: admin/.next/"
  echo ""
fi

if $BUILD_MOBILE; then
  echo ">>> Building mobile prod release APK..."
  cd "$ROOT/mobile"
  ./gradlew :composeApp:assembleProdRelease -PTAMIXA_API_BASE_URL="$API_URL"
  echo ">>> Mobile: mobile/composeApp/build/outputs/apk/prod/release/tamixa-prodRelease.apk"
  echo ""
fi

echo "== Build complete =="
