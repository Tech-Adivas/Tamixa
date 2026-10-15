#!/usr/bin/env bash
# Wrapper: builds shared Kotlin framework and symlinks to iOS app Frameworks.
# Run from repo root: ./scripts/link-ios-framework.sh [options]
# Passes all args to mobile/iosApp/scripts/link-shared-framework.sh

set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "$REPO_ROOT/mobile/iosApp/scripts/link-shared-framework.sh" "$@"
