# Legacy Android shell (not built)

The Gradle mobile project only includes **`mobile/composeApp`** (see repo root `settings.gradle.kts`). This `androidApp` tree is an older duplicate entry point and **`MainActivity` / `TamixaNavHost` here are not part of the shipping app**.

Use **`composeApp/src/androidMain/...`** for Android-specific code and **`composeApp/src/commonMain/.../navigation/TamixaNavHost.kt`** for navigation.
