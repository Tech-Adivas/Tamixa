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

/**
 * Railway usually provides `DATABASE_URL` (often `postgresql://...`) and `PGPASSWORD` / `PGUSER` separately.
 * After we set `spring.datasource.url` from DATABASE_URL or `SPRING_DATASOURCE_URL`, still copy PG* into
 * Spring properties when Spring-specific password env vars are not set.
 */
private fun applyDatasourceSecretsFromRailwayPgEnv(env: Map<String, String>) {
    val dsPassProp = System.getProperty("spring.datasource.password")?.takeIf { it.isNotBlank() }
    val dsUserProp = System.getProperty("spring.datasource.username")?.takeIf { it.isNotBlank() }
    val hasExplicitPassword =
        !env["SPRING_DATASOURCE_PASSWORD"].isNullOrBlank() ||
            !env["DATABASE_PASSWORD"].isNullOrBlank()
    val hasExplicitUser =
        !env["SPRING_DATASOURCE_USERNAME"].isNullOrBlank() ||
            !env["DATABASE_USERNAME"].isNullOrBlank()
    if (dsPassProp.isNullOrBlank() && !hasExplicitPassword) {
        val pgPass =
            env["PGPASSWORD"]?.takeIf { it.isNotBlank() }
                ?: env.entries.firstOrNull { it.key.equals("PGPASSWORD", ignoreCase = true) }?.value?.takeIf {
                    it.isNotBlank()
                }
        pgPass?.let { System.setProperty("spring.datasource.password", it) }
    }
    if (dsUserProp.isNullOrBlank() && !hasExplicitUser) {
        val pgUser =
            env["PGUSER"]?.takeIf { it.isNotBlank() }
                ?: env.entries.firstOrNull { it.key.equals("PGUSER", ignoreCase = true) }?.value?.takeIf {
                    it.isNotBlank()
                }
        pgUser?.let { System.setProperty("spring.datasource.username", it) }
    }
}

private fun applyDatabaseUrlCompatibility() {
    val env = System.getenv()
    // Prefer Railway's DATABASE_URL over SPRING_DATASOURCE_URL (Railway standard).
    val rawDatabaseUrl =
        env["DATABASE_URL"]?.takeIf { it.isNotBlank() }
            ?: env["DATABASE_PUBLIC_URL"]?.takeIf { it.isNotBlank() }

    if (!rawDatabaseUrl.isNullOrBlank()) {
        when {
            rawDatabaseUrl.startsWith("jdbc:postgresql:", ignoreCase = true) ||
                rawDatabaseUrl.startsWith("jdbc:postgres:", ignoreCase = true) -> {
                System.setProperty("spring.datasource.url", rawDatabaseUrl)
            }
            else -> applyJdbcPropertiesFromUrl(rawDatabaseUrl, env)
        }
    } else {
        env["SPRING_DATASOURCE_URL"]?.takeIf { it.isNotBlank() }?.let {
            System.setProperty("spring.datasource.url", it)
        } ?: applyPgHostCompatibility(env)
    }
    applyDatasourceSecretsFromRailwayPgEnv(env)
}

/**
 * Railway often omits SPRING_PROFILES_ACTIVE. Repo default in application.yml is `dev` (localhost DB), which
 * breaks containers. When any Railway runtime env is present, default the active profile to `staging` unless
 * already set (env, JVM -D, or CLI args). We also pass `--spring.profiles.active=staging` so it wins over YAML
 * defaults even if config resolution order differs from System.setProperty alone.
 */
private fun isRailwayDeployment(env: Map<String, String>): Boolean =
    env.entries.any { (key, value) -> key.startsWith("RAILWAY_") && !value.isNullOrBlank() }

private fun springProfilesActiveExplicitlySet(args: Array<String>): Boolean {
    val env = System.getenv()
    if (!env["SPRING_PROFILES_ACTIVE"].isNullOrBlank()) {
        return true
    }
    if (!System.getProperty("spring.profiles.active").isNullOrBlank()) {
        return true
    }
    var i = 0
    while (i < args.size) {
        val a = args[i]
        when {
            a.startsWith("--spring.profiles.active=") -> return true
            a == "--spring.profiles.active" && i + 1 < args.size -> return true
            a.startsWith("-Dspring.profiles.active=") -> return true
        }
        i++
    }
    return false
}

private fun applyRailwayDefaultProfile(args: Array<String>): Array<String> {
    if (springProfilesActiveExplicitlySet(args)) {
        return args
    }
    val env = System.getenv()
    if (!isRailwayDeployment(env)) {
        return args
    }
    System.setProperty("spring.profiles.active", "staging")
    log.info(
        "Railway deployment detected: defaulting active profile to staging " +
            "(set SPRING_PROFILES_ACTIVE, JVM -Dspring.profiles.active=..., or --spring.profiles.active=... to override)."
    )
    return arrayOf("--spring.profiles.active=staging", *args)
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
            !env["SPRING_DATASOURCE_PASSWORD"].isNullOrBlank() ||
            env.entries.any { it.key.equals("PGPASSWORD", ignoreCase = true) && it.value.isNotBlank() }
    val sysDsPassword = System.getProperty("spring.datasource.password")?.isNotBlank() == true
    val dbUrl = env["DATABASE_URL"]?.takeIf { it.isNotBlank() } ?: ""
    if (explicitPassword || sysDsPassword || databaseUrlAppearsToEmbedPassword(dbUrl)) {
        return
    }
    log.warn(
        "Active profile is staging/prod but no DB password was detected (DATABASE_PASSWORD / POSTGRES_PASSWORD / PGPASSWORD / SPRING_DATASOURCE_PASSWORD, or user:password inside DATABASE_URL). " +
            "On Railway, use the linked Postgres DATABASE_URL and PGPASSWORD (or a single DATABASE_URL with credentials). Hikari will usually fail with 'password authentication failed' or similar."
    )
}

private fun logLikelyDatasourceTarget() {
    val env = System.getenv()
    val activeRaw =
        System.getProperty("spring.profiles.active")?.takeIf { it.isNotBlank() }
            ?: env["SPRING_PROFILES_ACTIVE"]?.takeIf { it.isNotBlank() }
            ?: ""
    val stagingActive =
        activeRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.contains("staging")

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
                if (stagingActive && host == "localhost" && pgHost.isNullOrBlank()) {
                    // application-staging.yml defaults when POSTGRES_* / PG* are unset
                    "jdbc:postgresql://postgres.railway.internal:5432/railway"
                } else {
                    "jdbc:postgresql://$resolvedHost:$port/$db"
                }
            }
    val sanitized = url.replace(Regex("://[^/@]+@"), "://***@")
    log.info("Datasource target (pre-Spring bind, best-effort): {}", sanitized)

    val dsUrlSetAtStartup = System.getProperty("spring.datasource.url")?.isNotBlank() == true
    val hasJdbcFromDatabaseUrl =
        !env["DATABASE_URL"].isNullOrBlank() ||
            !env["DATABASE_PUBLIC_URL"].isNullOrBlank() ||
            !env["SPRING_DATASOURCE_URL"].isNullOrBlank()
    val hasPgHostEnv = !env["PGHOST"].isNullOrBlank()
    if (stagingActive && !dsUrlSetAtStartup && !hasJdbcFromDatabaseUrl && !hasPgHostEnv) {
        log.info(
            "Staging profile: no DATABASE_URL / DATABASE_PUBLIC_URL / SPRING_DATASOURCE_URL / PGHOST was applied before Spring starts; " +
                "the live datasource URL and credentials come from application-staging.yml " +
                "(default jdbc:postgresql://postgres.railway.internal:5432/railway plus DATABASE_USERNAME / POSTGRES_* / PGPASSWORD from env). " +
                "On Railway, prefer DATABASE_URL from the linked Postgres service."
        )
    } else if (sanitized.contains("localhost:5432") &&
        stagingActive
    ) {
        log.info(
            "Staging profile is active but datasource looks like localhost; set DATABASE_URL or Railway Postgres variables so staging does not use a local DB."
        )
    }
}

fun main(args: Array<String>) {
    val startupArgs = applyRailwayDefaultProfile(args)
    applyDatabaseUrlCompatibility()
    warnIfDatasourceEnvFamiliesOverlap()
    logLikelyDatasourceTarget()
    warnIfLikelyMissingDbCredentials()
    runApplication<TamixaApplication>(*startupArgs)
}
