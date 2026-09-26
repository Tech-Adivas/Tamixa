import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.InputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    id("com.google.gms.google-services") version "4.4.2"
    id("com.google.firebase.crashlytics") version "3.0.2"
}

compose.resources {
    packageOfResClass = "com.tamixa.composeapp.generated.resources"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        iosTarget.compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
                }
            }
        }
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.logging)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.core.coroutines)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                // Pin to 3.0.3: 3.4.x is built with Kotlin 2.3 (ABI 2.3.0), incompatible with Kotlin 2.2.x
                implementation("io.coil-kt.coil3:coil-gif:3.0.3")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.firebase.crashlytics)
                // Route SLF4J (used by Ktor and shared code) to Android logcat
                implementation("org.slf4j:slf4j-android:1.7.36")
                implementation(libs.androidx.security.crypto)
                implementation(libs.androidx.compose.ui.tooling.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.session)
                implementation(libs.androidx.media3.ui)
                implementation(libs.androidx.media3.datasource)
                implementation(libs.androidx.core.ktx)
                implementation("androidx.core:core-splashscreen:1.2.0")
                implementation(libs.androidx.datastore)
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
            }
        }
    }
}

/** Tamixa URL overrides: prefer mobile/local.properties (monorepo), else repo/mobile-root local.properties. */
val tamixaLocalProps: Properties by lazy {
    val p = Properties()
    val mobileLocal = rootProject.file("mobile/local.properties")
    val rootLocal = rootProject.file("local.properties")
    val stream: InputStream? = when {
        mobileLocal.exists() -> mobileLocal.inputStream()
        rootLocal.exists() -> rootLocal.inputStream()
        else -> null
    }
    stream?.use { p.load(it) }
    p
}

fun tamixaDevApiBase(): String =
    (tamixaLocalProps["TAMIXA_API_BASE_URL"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: "http://10.0.2.2:8080"

fun tamixaDevWebRoot(): String {
    val api = tamixaDevApiBase()
    return (tamixaLocalProps["TAMIXA_WEB_APP_URL"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: api.replaceAfterLast(":", "3000")
}

/**
 * Play Store upload key. Read from local.properties or environment (CI) — never commit the keystore or passwords.
 *   TAMIXA_UPLOAD_STORE_FILE=/abs/path/tamixa-upload.jks
 *   TAMIXA_UPLOAD_STORE_PASSWORD=...
 *   TAMIXA_UPLOAD_KEY_ALIAS=tamixa
 *   TAMIXA_UPLOAD_KEY_PASSWORD=...
 * When unset, release builds stay unsigned (debug builds are unaffected).
 */
fun tamixaSigningValue(key: String): String? =
    ((tamixaLocalProps[key] as? String) ?: System.getenv(key))?.trim()?.takeIf { it.isNotEmpty() }

val tamixaUploadStoreFile: File? =
    tamixaSigningValue("TAMIXA_UPLOAD_STORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "com.tamixa.android"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.tamixa.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            isDefault = true
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Tamixa (Dev)")
            val api = tamixaDevApiBase()
            val webRoot = tamixaDevWebRoot()
            buildConfigField("String", "BASE_URL", "\"$api\"")
            buildConfigField("String", "SUBSCRIPTION_WEB_URL", "\"$webRoot/subscription\"")
            buildConfigField("String", "TAMIXA_ENVIRONMENT", "\"dev\"")
        }
        create("qa") {
            dimension = "environment"
            versionNameSuffix = "-qa"
            resValue("string", "app_name", "Tamixa (QA)")
            val api = (project.findProperty("TAMIXA_QA_API_BASE_URL") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "https://api-qa.tamixa.com"
            val webRoot = (project.findProperty("TAMIXA_QA_WEB_APP_URL") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "https://app-qa.tamixa.com"
            buildConfigField("String", "BASE_URL", "\"$api\"")
            buildConfigField("String", "SUBSCRIPTION_WEB_URL", "\"$webRoot/subscription\"")
            buildConfigField("String", "TAMIXA_ENVIRONMENT", "\"qa\"")
        }
        create("prod") {
            dimension = "environment"
            val api = (project.findProperty("TAMIXA_API_BASE_URL") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "https://api.tamixa.in"
            val webRoot = (project.findProperty("TAMIXA_WEB_APP_URL") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "https://app.tamixa.com"
            buildConfigField("String", "BASE_URL", "\"$api\"")
            buildConfigField("String", "SUBSCRIPTION_WEB_URL", "\"$webRoot/subscription\"")
            buildConfigField("String", "TAMIXA_ENVIRONMENT", "\"prod\"")
        }
    }

    signingConfigs {
        if (tamixaUploadStoreFile != null) {
            create("release") {
                storeFile = tamixaUploadStoreFile
                storePassword = tamixaSigningValue("TAMIXA_UPLOAD_STORE_PASSWORD")
                keyAlias = tamixaSigningValue("TAMIXA_UPLOAD_KEY_ALIAS")
                keyPassword = tamixaSigningValue("TAMIXA_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (tamixaUploadStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Custom APK name: tamixa-debug.apk, tamixa-release.apk
android.applicationVariants.all {
    val variant = this
    outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            "tamixa-${variant.name}.apk"
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// With product flavors, AGP does not expose a unique `installDebug`; default local flow = dev.
tasks.register("installDebug") {
    group = "Install"
    description = "Installs devDebug on a device (use installQaDebug / installProdDebug for other flavors)."
    dependsOn("installDevDebug")
}
