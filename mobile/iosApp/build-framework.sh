#!/usr/bin/env bash
# Build the shared Kotlin framework and copy it to Frameworks/ so Xcode can find it.
# Run once from repo root before opening Xcode, or let the Xcode Run Script do it on first build.
set -e
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"
echo "Building shared framework (Debug, iOS Simulator)..."
./gradlew :mobile:composeApp:linkDebugFrameworkIosSimulatorArm64 -Pkotlin.native.cacheKind=none -Ptamixa.iosOnly=true
FRAMEWORK_SRC="mobile/composeApp/build/bin/iosSimulatorArm64/debugFramework/shared.framework"
FRAMEWORK_DST="mobile/iosApp/Tamixa/Frameworks/shared.framework"
mkdir -p "mobile/iosApp/Tamixa/Frameworks"
rm -rf "$FRAMEWORK_DST"
cp -R "$FRAMEWORK_SRC" "$FRAMEWORK_DST"
echo "Done. Framework copied to $FRAMEWORK_DST"
echo "You can now open Tamixa.xcodeproj in Xcode and build."
