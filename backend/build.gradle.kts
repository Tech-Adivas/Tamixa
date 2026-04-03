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

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

/** Staging/prod use DATABASE_PUBLIC_URL / DATABASE_URL in the environment; CI does not write `.env`. Prefer OS env, then repo `.env`. */
fun pickEnv(vararg keys: String): String? {
    for (k in keys) {
        System.getenv(k)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        envMap[k]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return null
}

/**
 * Same URL choice as [com.tamixa.TamixaApplication.applyDatabaseUrlCompatibility]:
 * on Railway prefer private DATABASE_URL (what the backend service usually uses); off Railway prefer DATABASE_PUBLIC_URL
 * so laptops are not forced to use internal hostnames. Mismatches here cause "migrate succeeded" but empty tables on the app DB.
 */
fun pickPrimaryDatabaseUrlForFlyway(): String? =
    if (System.getenv().entries.any { (k, v) -> k.startsWith("RAILWAY_") && v.isNotBlank() }) {
        pickEnv("DATABASE_URL", "DATABASE_PUBLIC_URL")
    } else {
        pickEnv("DATABASE_PUBLIC_URL", "DATABASE_URL")
    }

fun normalizeFlywayJdbcUrl(raw: String): String {
    val u = raw.trim()
    if (u.startsWith("jdbc:")) return u
    if (u.startsWith("postgres://")) return "jdbc:postgresql://" + u.removePrefix("postgres://")
    if (u.startsWith("postgresql://")) return "jdbc:postgresql://" + u.removePrefix("postgresql://")
    return u
}

private fun decodeUserInfoPart(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8)

/**
 * JDBC URLs must not embed user:password@ before the host — the PostgreSQL JDBC driver often fails to connect
 * (08001) even with sslmode=require. Align with [com.tamixa.TamixaApplication] libpq-style handling.
 */
fun stripJdbcPostgresqlUserInfo(jdbcUrl: String): Triple<String, String?, String?> {
    val schemeMarker = "://"
    val idx = jdbcUrl.indexOf(schemeMarker)
    if (idx < 0) return Triple(jdbcUrl, null, null)
    val schemePart = jdbcUrl.substring(0, idx).lowercase()
    if (!schemePart.startsWith("jdbc:postgresql") && !schemePart.startsWith("jdbc:postgres")) {
        return Triple(jdbcUrl, null, null)
    }
    val afterScheme = idx + schemeMarker.length
    val schemePrefix = jdbcUrl.substring(0, afterScheme)
    val rest = jdbcUrl.substring(afterScheme)
    val atIdx = rest.indexOf('@')
    if (atIdx < 0) return Triple(jdbcUrl, null, null)
    if (rest.substring(0, atIdx).contains('/')) {
        return Triple(jdbcUrl, null, null)
    }
    val userInfo = rest.substring(0, atIdx)
    val afterAt = rest.substring(atIdx + 1)
    val user = userInfo.substringBefore(':').takeIf { it.isNotBlank() }
    val pass = userInfo.substringAfter(':', "").takeIf { it.isNotEmpty() }
    return Triple(schemePrefix + afterAt, user, pass)
}

/** Match [com.tamixa.TamixaApplication] startup: Railway public proxy requires TLS for JDBC. */
fun ensureRailwayPublicJdbcUrlHasSsl(jdbcUrl: String): String {
    if (!jdbcUrl.startsWith("jdbc:postgresql:", ignoreCase = true) &&
        !jdbcUrl.startsWith("jdbc:postgres:", ignoreCase = true)
    ) {
        return jdbcUrl
    }
    if (jdbcUrl.contains("sslmode=", ignoreCase = true)) {
        return jdbcUrl
    }
    val schemeSep = jdbcUrl.indexOf("://")
    if (schemeSep < 0) {
        return jdbcUrl
    }
    val afterScheme = jdbcUrl.substring(schemeSep + 3)
    val hostPart = afterScheme.substringBefore("/").substringBefore("?")
    if (hostPart.isBlank()) {
        return jdbcUrl
    }
    val needsSsl =
        hostPart.contains("proxy.rlwy.net", ignoreCase = true) ||
            (hostPart.endsWith(".rlwy.net", ignoreCase = true) && !hostPart.contains("railway.internal", ignoreCase = true))
    if (!needsSsl) {
        return jdbcUrl
    }
    return if (jdbcUrl.contains("?")) "$jdbcUrl&sslmode=require" else "$jdbcUrl?sslmode=require"
}

fun resolveFlywayJdbcAndCredentials(): Triple<String, String?, String?> {
    val fromVars = pickPrimaryDatabaseUrlForFlyway()
    if (fromVars == null) {
        val host = pickEnv("POSTGRES_HOST") ?: "localhost"
        val port = pickEnv("POSTGRES_PORT") ?: "5432"
        val db = pickEnv("POSTGRES_DB") ?: "araro_kids"
        val base = "jdbc:postgresql://$host:$port/$db"
        return Triple(ensureRailwayPublicJdbcUrlHasSsl(base), null, null)
    }
    val trimmed = fromVars.trim()
    if (trimmed.startsWith("postgresql://", ignoreCase = true) ||
        trimmed.startsWith("postgres://", ignoreCase = true)
    ) {
        val uri = URI(trimmed)
        val databaseName =
            uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() }
                ?: error("flyway: postgres:// DATABASE_URL must include a database name in the path")
        val host = uri.host ?: error("flyway: postgres:// DATABASE_URL must include a host")
        val port = if (uri.port == -1) 5432 else uri.port
        val query = uri.rawQuery
        val userFromUrl = uri.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() }?.let(::decodeUserInfoPart)
        val passFromUrl =
            uri.userInfo
                ?.substringAfter(':', "")
                ?.takeIf { it.isNotBlank() }
                ?.let(::decodeUserInfoPart)
        val jdbcBase = buildString {
            append("jdbc:postgresql://")
            append(host)
            append(':')
            append(port)
            append('/')
            append(databaseName)
            if (!query.isNullOrBlank()) {
                append('?')
                append(query)
            }
        }
        return Triple(ensureRailwayPublicJdbcUrlHasSsl(jdbcBase), userFromUrl, passFromUrl)
    }
    val jdbcRaw = normalizeFlywayJdbcUrl(trimmed)
    val (jdbcWithoutUser, u, p) = stripJdbcPostgresqlUserInfo(jdbcRaw)
    return Triple(ensureRailwayPublicJdbcUrlHasSsl(jdbcWithoutUser), u, p)
}

val flywayResolved = resolveFlywayJdbcAndCredentials()
val flywayUrl = flywayResolved.first
val flywayUser = pickEnv("DATABASE_USERNAME", "POSTGRES_USER", "PGUSER") ?: flywayResolved.second ?: "postgres"
val flywayPassword =
    pickEnv("DATABASE_PASSWORD", "POSTGRES_PASSWORD", "PGPASSWORD") ?: flywayResolved.third ?: "postgres"

flyway {
    url = flywayUrl
    user = flywayUser
    password = flywayPassword
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
    schemas = arrayOf("public")
    createSchemas = true
}

tasks.register("flywayShowTarget") {
    group = "flyway"
    description =
        "Print JDBC URL and user Flyway will use (no password). Run before migrate to confirm you hit the same DB as staging."
    doLast {
        println("Flyway URL: $flywayUrl")
        println("Flyway user: $flywayUser")
        val onRailway = System.getenv().entries.any { (k, v) -> k.startsWith("RAILWAY_") && v.isNotBlank() }
        println(
            "URL source order: " +
                if (onRailway) {
                    "RAILWAY_* detected → DATABASE_URL, then DATABASE_PUBLIC_URL (matches TamixaApplication)."
                } else {
                    "no RAILWAY_* → DATABASE_PUBLIC_URL, then DATABASE_URL (matches TamixaApplication off Railway)."
                }
        )
    }
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
    // Avoid multiple JVMs each starting Postgres (resource-heavy); keep one fork unless changed deliberately.
    maxParallelForks = 1
    // Local shells often export SPRING_DATASOURCE_* for bootRun; those override @DynamicPropertySource and break
    // Testcontainers (stale port / wrong DB). CI sets CI=true and provides the service Postgres URL on purpose.
    if (System.getenv("CI") != "true") {
        val env = System.getenv().toMutableMap()
        env.keys.filter { key ->
            key.equals("SPRING_DATASOURCE_URL", ignoreCase = true) ||
                key.equals("SPRING_DATASOURCE_USERNAME", ignoreCase = true) ||
                key.equals("SPRING_DATASOURCE_PASSWORD", ignoreCase = true)
        }.forEach { env.remove(it) }
        @Suppress("UNCHECKED_CAST")
        environment = env as MutableMap<String, Any>
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
