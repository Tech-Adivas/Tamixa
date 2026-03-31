package com.tamixa

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.slf4j.LoggerFactory

@SpringBootApplication
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactory")
@EnableRetry
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
class TamixaApplication

private val log = LoggerFactory.getLogger("TamixaStartup")

private fun decodeUserInfoPart(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

private fun applyJdbcPropertiesFromUrl(rawUrl: String, env: Map<String, String>) {
    val looksLikeRailwayStyleUrl =
        rawUrl.startsWith("postgresql://", ignoreCase = true) ||
            rawUrl.startsWith("postgres://", ignoreCase = true)
    if (!looksLikeRailwayStyleUrl) {
        return
    }
    val uri =
        try {
            URI(rawUrl)
        } catch (e: Exception) {
            log.error("Failed to parse DATABASE_URL for datasource normalization", e)
            return
        }
    val databaseName = uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() }
    if (databaseName.isNullOrBlank() || uri.host.isNullOrBlank()) {
        log.warn("DATABASE_URL is missing host or database name; skipping datasource normalization")
        return
    }
    val jdbcUrl =
        buildString {
            append("jdbc:postgresql://")
            append(uri.host)
            append(':')
            append(if (uri.port == -1) 5432 else uri.port)
            append('/')
            append(databaseName)
            if (!uri.rawQuery.isNullOrBlank()) {
                append('?')
                append(uri.rawQuery)
            }
        }
    System.setProperty("spring.datasource.url", jdbcUrl)

    if (env["SPRING_DATASOURCE_USERNAME"].isNullOrBlank() && env["DATABASE_USERNAME"].isNullOrBlank()) {
        val username = uri.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() }?.let(::decodeUserInfoPart)
        if (!username.isNullOrBlank()) {
            System.setProperty("spring.datasource.username", username)
        }
    }
    if (env["SPRING_DATASOURCE_PASSWORD"].isNullOrBlank() && env["DATABASE_PASSWORD"].isNullOrBlank()) {
        val password =
            uri.userInfo
                ?.substringAfter(':', missingDelimiterValue = "")
                ?.takeIf { it.isNotBlank() }
                ?.let(::decodeUserInfoPart)
        if (!password.isNullOrBlank()) {
            System.setProperty("spring.datasource.password", password)
        }
    }
    log.info("Normalized DATABASE_URL to JDBC datasource URL for Spring Boot startup")
}

private fun applyPgHostCompatibility(env: Map<String, String>) {
    val pgHost = env["PGHOST"]?.takeIf { it.isNotBlank() } ?: return
    val pgPort = env["PGPORT"]?.takeIf { it.isNotBlank() } ?: "5432"
    var pgDatabase =
        env["PGDATABASE"]?.takeIf { it.isNotBlank() }
            ?: env["POSTGRES_DB"]?.takeIf { it.isNotBlank() }
    if (pgDatabase.isNullOrBlank() && pgHost.contains("railway.internal", ignoreCase = true)) {
        pgDatabase = "railway"
    }
    if (pgDatabase.isNullOrBlank()) {
        return
    }
    val jdbcUrl = "jdbc:postgresql://$pgHost:$pgPort/$pgDatabase"
    System.setProperty("spring.datasource.url", jdbcUrl)

    if (env["SPRING_DATASOURCE_USERNAME"].isNullOrBlank() && env["DATABASE_USERNAME"].isNullOrBlank()) {
        env["PGUSER"]?.takeIf { it.isNotBlank() }?.let { System.setProperty("spring.datasource.username", it) }
    }
    if (env["SPRING_DATASOURCE_PASSWORD"].isNullOrBlank() && env["DATABASE_PASSWORD"].isNullOrBlank()) {
        env["PGPASSWORD"]?.takeIf { it.isNotBlank() }?.let { System.setProperty("spring.datasource.password", it) }
    }
    log.info("Configured datasource from PG* environment variables")
}

private fun applyDatabaseUrlCompatibility() {
    val env = System.getenv()
    val springDatasourceUrlEnv = env["SPRING_DATASOURCE_URL"]?.takeIf { it.isNotBlank() }
    if (!springDatasourceUrlEnv.isNullOrBlank()) {
        return
    }

    val rawDatabaseUrl =
        env["DATABASE_URL"]?.takeIf { it.isNotBlank() }
            ?: env["DATABASE_PUBLIC_URL"]?.takeIf { it.isNotBlank() }

    if (!rawDatabaseUrl.isNullOrBlank()) {
        applyJdbcPropertiesFromUrl(rawDatabaseUrl, env)
        return
    }
    applyPgHostCompatibility(env)
}

/**
 * Railway often omits SPRING_PROFILES_ACTIVE. Repo default in application.yml is `dev` (localhost DB), which
 * breaks containers. When Railway env vars are present, default the active profile to `staging` unless set.
 */
private fun applyRailwayDefaultProfile() {
    val env = System.getenv()
    val fromEnv = env["SPRING_PROFILES_ACTIVE"]?.takeIf { it.isNotBlank() }
    val fromProperty = System.getProperty("spring.profiles.active")?.takeIf { it.isNotBlank() }
    if (!fromEnv.isNullOrBlank() || !fromProperty.isNullOrBlank()) {
        return
    }
    val onRailway =
        !env["RAILWAY_ENVIRONMENT"].isNullOrBlank() ||
            !env["RAILWAY_ENVIRONMENT_ID"].isNullOrBlank() ||
            env["RAILWAY"] == "true"
    if (!onRailway) {
        return
    }
    System.setProperty("spring.profiles.active", "staging")
    log.info(
        "Railway deployment detected: defaulting spring.profiles.active=staging (set SPRING_PROFILES_ACTIVE to override)."
    )
}

private fun warnIfDatasourceEnvFamiliesOverlap() {
    val env = System.getenv()
    val hasDatabaseFamily =
        !env["DATABASE_URL"].isNullOrBlank() ||
            !env["DATABASE_USERNAME"].isNullOrBlank() ||
            !env["DATABASE_PASSWORD"].isNullOrBlank()
    val hasPostgresFamily =
        !env["POSTGRES_HOST"].isNullOrBlank() ||
            !env["POSTGRES_PORT"].isNullOrBlank() ||
            !env["POSTGRES_DB"].isNullOrBlank() ||
            !env["POSTGRES_USER"].isNullOrBlank() ||
            !env["POSTGRES_PASSWORD"].isNullOrBlank()
    if (hasDatabaseFamily && hasPostgresFamily) {
        log.warn(
            "Both DATABASE_* and POSTGRES_* env vars are set. Ensure they point to the same DB to avoid inconsistent data reads/writes across runs."
        )
    }
}

private fun databaseUrlAppearsToEmbedPassword(raw: String): Boolean {
    if (!raw.startsWith("postgresql://", ignoreCase = true) && !raw.startsWith("postgres://", ignoreCase = true)) {
        return false
    }
    return try {
        val uri = URI(raw)
        val ui = uri.userInfo
        ui != null && ui.contains(":") && ui.substringAfter(":").isNotBlank()
    } catch (_: Exception) {
        false
    }
}

private fun warnIfLikelyMissingDbCredentials() {
    val activeRaw = System.getProperty("spring.profiles.active") ?: System.getenv("SPRING_PROFILES_ACTIVE") ?: ""
    val profiles = activeRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
    if (!profiles.contains("staging") && !profiles.contains("prod")) {
        return
    }
    val env = System.getenv()
    val explicitPassword =
        !env["DATABASE_PASSWORD"].isNullOrBlank() ||
            !env["POSTGRES_PASSWORD"].isNullOrBlank() ||
            !env["PGPASSWORD"].isNullOrBlank() ||
            !env["SPRING_DATASOURCE_PASSWORD"].isNullOrBlank()
    val sysDsPassword = System.getProperty("spring.datasource.password")?.isNotBlank() == true
    val dbUrl = env["DATABASE_URL"]?.takeIf { it.isNotBlank() } ?: ""
    if (explicitPassword || sysDsPassword || databaseUrlAppearsToEmbedPassword(dbUrl)) {
        return
    }
    log.warn(
        "Active profile is staging/prod but no DB password was detected (DATABASE_PASSWORD / POSTGRES_PASSWORD / PGPASSWORD / SPRING_DATASOURCE_PASSWORD, or user:password in DATABASE_URL). " +
            "Link Postgres in Railway or set these vars — Hikari will usually fail with 'password authentication failed' or similar."
    )
}

private fun logLikelyDatasourceTarget() {
    val env = System.getenv()
    val url =
        System.getProperty("spring.datasource.url")?.takeIf { it.isNotBlank() }
            ?: env["SPRING_DATASOURCE_URL"]?.takeIf { it.isNotBlank() }
            ?: env["DATABASE_URL"]?.takeIf { it.isNotBlank() }
            ?: run {
                val host = env["POSTGRES_HOST"]?.takeIf { it.isNotBlank() } ?: "localhost"
                val pgHost = env["PGHOST"]?.takeIf { it.isNotBlank() }
                val port = env["POSTGRES_PORT"]?.takeIf { it.isNotBlank() } ?: env["PGPORT"]?.takeIf { it.isNotBlank() } ?: "5432"
                val db = env["POSTGRES_DB"]?.takeIf { it.isNotBlank() } ?: env["PGDATABASE"]?.takeIf { it.isNotBlank() } ?: "araro_kids"
                val resolvedHost = pgHost ?: host
                "jdbc:postgresql://$resolvedHost:$port/$db"
            }
    val sanitized = url.replace(Regex("://[^/@]+@"), "://***@")
    log.info("Datasource target (resolved from env): {}", sanitized)
    val active =
        System.getProperty("spring.profiles.active")?.takeIf { it.isNotBlank() }
            ?: env["SPRING_PROFILES_ACTIVE"]?.takeIf { it.isNotBlank() }
    if (sanitized.contains("localhost:5432") == true &&
        active?.split(",")?.any { it.trim().equals("staging", ignoreCase = true) } == true
    ) {
        log.info(
            "Profile staging is active: if DATABASE_URL/PG* are unset, Spring loads jdbc URL from application-staging.yml (Railway internal host defaults)."
        )
    }
}

fun main(args: Array<String>) {
    applyRailwayDefaultProfile()
    applyDatabaseUrlCompatibility()
    warnIfDatasourceEnvFamiliesOverlap()
    logLikelyDatasourceTarget()
    warnIfLikelyMissingDbCredentials()
    runApplication<TamixaApplication>(*args)
}
