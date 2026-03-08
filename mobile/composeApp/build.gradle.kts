import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    packageOfResClass = "com.araro.composeapp.generated.resources"
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
            compilerOptions.configure {
                freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
            }
        }
    }

    sourceSets {
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
                implementation("io.coil-kt.coil3:coil-gif:3.4.0")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.firebase.crashlytics)
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
                implementation("androidx.core:core-splashscreen:1.0.1")
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

android {
    namespace = "com.araro.android"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.araro.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // Physical device: set ARARO_API_BASE_URL in mobile/local.properties (e.g. http://192.168.1.5:8080)
        val localProps = java.util.Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())
        val apiBaseUrl = (localProps["ARARO_API_BASE_URL"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: "http://10.0.2.2:8080"
        val subscriptionWebUrl = (localProps["ARARO_WEB_APP_URL"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: apiBaseUrl.replaceAfterLast(":", "3000")
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "SUBSCRIPTION_WEB_URL", "\"$subscriptionWebUrl/subscription\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val prodUrl = project.findProperty("ARARO_API_BASE_URL") as? String
                ?: "https://api.araro.com"
            val webUrl = project.findProperty("ARARO_WEB_APP_URL") as? String
                ?: "https://app.araro.com"
            buildConfigField("String", "BASE_URL", "\"$prodUrl\"")
            buildConfigField("String", "SUBSCRIPTION_WEB_URL", "\"$webUrl/subscription\"")
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

// Custom APK name: araro-debug.apk, araro-release.apk
android.applicationVariants.all {
    val variant = this
    outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            "araro-${variant.name}.apk"
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)
}
