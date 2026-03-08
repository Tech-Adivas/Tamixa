rootProject.name = "araro"

/** When true, excludes mobile (requires Android SDK; has Kotlin/AGP compatibility issues). Use for backend-only builds. */
val backendOnly = providers.gradleProperty("araro.backendOnly").orElse("false").get().toBooleanStrictOrNull() ?: false
/** When true, includes only mobile/composeApp (e.g. when building iOS framework from Xcode). Avoids configuring backend. */
val iosOnly = providers.gradleProperty("araro.iosOnly").orElse("false").get().toBooleanStrictOrNull() ?: false

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

if (!iosOnly) {
    include(":backend")
    include(":web")
}
if (!backendOnly || iosOnly) {
    include(":mobile")
    include(":mobile:composeApp")
}
