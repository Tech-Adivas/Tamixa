#!/usr/bin/env bash
# Creates Araro/Frameworks/shared.framework as a symlink to the Gradle-built framework.
# Run from repo root: ./mobile/iosApp/scripts/link-shared-framework.sh
# Or from mobile/iosApp: ./scripts/link-shared-framework.sh
# Options: --simulator (default) | --device; --debug (default) | --release; --link-only (skip Gradle build)

set -e

SDK="simulator"
CONFIG="debug"
LINK_ONLY=""

for arg in "$@"; do
  case "$arg" in
    --device)     SDK="device" ;;
    --simulator)  SDK="simulator" ;;
    --release)    CONFIG="release" ;;
    --debug)      CONFIG="debug" ;;
    --link-only)  LINK_ONLY=1 ;;
    -h|--help)
      echo "Usage: $0 [--simulator|--device] [--debug|--release] [--link-only]"
      echo "  Defaults: --simulator --debug"
      echo "  Builds the shared Kotlin framework and symlinks it to Araro/Frameworks/shared.framework"
      echo "  Use --link-only to only create the symlink (framework must already be built)."
      exit 0
      ;;
  esac
done

# Repo root: script may be at mobile/iosApp/scripts/link-shared-framework.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$IOS_APP_DIR/../.." && pwd)"

cd "$REPO_ROOT"

[ "$CONFIG" = "debug" ] && CONFIG_CAP="Debug" || CONFIG_CAP="Release"
if [ "$SDK" = "simulator" ]; then
  TASK=":mobile:composeApp:link${CONFIG_CAP}FrameworkIosSimulatorArm64"
  FRAMEWORK_SRC="mobile/composeApp/build/bin/iosSimulatorArm64/${CONFIG}Framework/shared.framework"
else
  TASK=":mobile:composeApp:link${CONFIG_CAP}FrameworkIosArm64"
  FRAMEWORK_SRC="mobile/composeApp/build/bin/iosArm64/${CONFIG}Framework/shared.framework"
fi

if [ -z "$LINK_ONLY" ]; then
  if [ ! -x "./gradlew" ]; then
    echo "Error: gradlew not found in $REPO_ROOT"
    exit 1
  fi
  echo "Building framework: $TASK"
  ./gradlew "$TASK" -Pkotlin.native.cacheKind=none -Pararo.iosOnly=true
fi

ABS_SRC="$(pwd)/$FRAMEWORK_SRC"
if [ ! -f "$ABS_SRC/Headers/shared.h" ]; then
  echo "Error: framework missing Headers/shared.h at $ABS_SRC"
  exit 1
fi

FRAMEWORK_DST="$IOS_APP_DIR/Araro/Frameworks/shared.framework"
mkdir -p "$(dirname "$FRAMEWORK_DST")"
rm -rf "$FRAMEWORK_DST"
ln -s "$ABS_SRC" "$FRAMEWORK_DST"
echo "Linked: $FRAMEWORK_DST -> $ABS_SRC"
exit 0
