plugins {
    id("org.springframework.boot") version "4.0.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("android") version "2.2.21" apply false
    kotlin("multiplatform") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("com.android.application") version "8.9.3" apply false
}

allprojects {
    group = "com.tamixa"
    version = "0.0.1-SNAPSHOT"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
