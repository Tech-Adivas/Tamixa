buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:10.8.1")
    }
}

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
    id("org.flywaydb.flyway") version "10.8.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.13.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.13.0")
    implementation("io.micrometer:micrometer-core:1.12.5")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    implementation("software.amazon.awssdk:s3:2.25.0")

    /** Decode MP3 length for narration duration + truncation_warning (per-segment and single-blob TTS). */
    implementation("com.mpatric:mp3agic:0.9.1")

    implementation("com.google.cloud:google-cloud-speech:4.81.0")

    implementation("com.stripe:stripe-java:24.0.0")
    implementation("org.flywaydb:flyway-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

// Load .env from project root (for bootRun and Flyway)
fun loadEnv(): Map<String, String> {
    val envFile = rootProject.projectDir.resolve(".env")
    if (!envFile.exists()) return emptyMap()
    return envFile.readLines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) return@mapNotNull null
        val eq = trimmed.indexOf('=')
        val key = trimmed.substring(0, eq).trim()
        var value = trimmed.substring(eq + 1).trim()
        if (!value.startsWith("\"") && !value.startsWith("'")) {
            val hash = value.indexOf('#')
            if (hash >= 0) value = value.substring(0, hash).trim()
        }
        if (value.startsWith("\"") && value.endsWith("\"")) value = value.drop(1).dropLast(1)
        if (value.startsWith("'") && value.endsWith("'")) value = value.drop(1).dropLast(1)
        key to value
    }.toMap()
}

val envMap = loadEnv()
val flywayUrl = envMap["DATABASE_URL"] ?: "jdbc:postgresql://${envMap["POSTGRES_HOST"] ?: "localhost"}:${envMap["POSTGRES_PORT"] ?: "5432"}/${envMap["POSTGRES_DB"] ?: "araro_kids"}"
val flywayUser = envMap["POSTGRES_USER"] ?: envMap["DATABASE_USERNAME"] ?: "postgres"
val flywayPassword = envMap["POSTGRES_PASSWORD"] ?: envMap["DATABASE_PASSWORD"] ?: "postgres"

flyway {
    url = flywayUrl
    user = flywayUser
    password = flywayPassword
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
}

// Load .env from project root into bootRun so AWS_*, S3_*, etc. are available when running locally
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms256m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=build/heap-dump.hprof"
    )
    doFirst {
        val envFile = rootProject.projectDir.resolve(".env")
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val eq = trimmed.indexOf('=')
                    val key = trimmed.substring(0, eq).trim()
                    var value = trimmed.substring(eq + 1).trim()
                    // Strip inline # comment (value must not be quoted for comment to apply)
                    if (!value.startsWith("\"") && !value.startsWith("'")) {
                        val hash = value.indexOf('#')
                        if (hash >= 0) value = value.substring(0, hash).trim()
                    }
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.drop(1).dropLast(1)
                    }
                    if (key.isNotEmpty()) environment(key, value)
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
