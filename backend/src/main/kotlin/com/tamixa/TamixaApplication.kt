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

/** Railway / shells sometimes vary env key casing; JVM lookup is case-sensitive on Linux. */
private fun envValueIgnoringCase(env: Map<String, String>, key: String): String? {
    env[key]?.takeIf { it.isNotBlank() }?.let { return it }
    return env.entries.firstOrNull { it.key.equals(key, ignoreCase = true) && it.value.isNotBlank() }?.value
}

private fun decodeUserInfoPart(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

/** Railway public proxy hosts typically require TLS; append sslmode=require if missing. */
private fun ensureRailwayPublicJdbcUrlHasSsl(jdbcUrl: String): String {
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
    val host = uri.host ?: ""
    val query = uri.rawQuery
    val needsPublicSsl =
        (query.isNullOrBlank() || !query.contains("sslmode", ignoreCase = true)) &&
            (host.contains("proxy.rlwy.net", ignoreCase = true) ||
                (host.endsWith(".rlwy.net", ignoreCase = true) && !host.contains("railway.internal", ignoreCase = true)))
    val jdbcUrl =
        buildString {
            append("jdbc:postgresql://")
            append(host)
            append(':')
            append(if (uri.port == -1) 5432 else uri.port)
            append('/')
            append(databaseName)
            when {
                !query.isNullOrBlank() && needsPublicSsl -> {
                    append('?')
                    append(query)
                    append("&sslmode=require")
                }
                !query.isNullOrBlank() -> {
                    append('?')
                    append(query)
                }
                needsPublicSsl -> append("?sslmode=require")
            }
        }
    System.setProperty("spring.datasource.url", jdbcUrl)

    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_USERNAME").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_USERNAME").isNullOrBlank()
    ) {
        val username = uri.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() }?.let(::decodeUserInfoPart)
        if (!username.isNullOrBlank()) {
            System.setProperty("spring.datasource.username", username)
        }
    }
    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank()
    ) {
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
    val pgHost = envValueIgnoringCase(env, "PGHOST") ?: return
    val pgPort = envValueIgnoringCase(env, "PGPORT") ?: "5432"
    var pgDatabase =
        envValueIgnoringCase(env, "PGDATABASE")
            ?: envValueIgnoringCase(env, "POSTGRES_DB")
    if (pgDatabase.isNullOrBlank() && pgHost.contains("railway.internal", ignoreCase = true)) {
        pgDatabase = "railway"
    }
    if (pgDatabase.isNullOrBlank()) {
        return
    }
    val jdbcUrl = "jdbc:postgresql://$pgHost:$pgPort/$pgDatabase"
    System.setProperty("spring.datasource.url", jdbcUrl)

    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_USERNAME").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_USERNAME").isNullOrBlank()
    ) {
        envValueIgnoringCase(env, "PGUSER")?.let { System.setProperty("spring.datasource.username", it) }
    }
    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank()
    ) {
        envValueIgnoringCase(env, "PGPASSWORD")?.let { System.setProperty("spring.datasource.password", it) }
    }
    log.info("Configured datasource from PG* environment variables")
}

/**
 * Railway often provides both `DATABASE_URL` (private / internal) and `DATABASE_PUBLIC_URL` (TCP proxy).
 * We prefer **DATABASE_PUBLIC_URL** when set so the app connects from services without Private Networking.
 * After we set `spring.datasource.url`, still copy PG* into Spring properties when Spring-specific env is unset.
 */
private fun applyDatasourceSecretsFromRailwayPgEnv(env: Map<String, String>) {
    val dsPassProp = System.getProperty("spring.datasource.password")?.takeIf { it.isNotBlank() }
    val dsUserProp = System.getProperty("spring.datasource.username")?.takeIf { it.isNotBlank() }
    val hasExplicitPassword =
        !envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank() ||
            !envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank()
    val hasExplicitUser =
        !envValueIgnoringCase(env, "SPRING_DATASOURCE_USERNAME").isNullOrBlank() ||
            !envValueIgnoringCase(env, "DATABASE_USERNAME").isNullOrBlank()
    if (dsPassProp.isNullOrBlank() && !hasExplicitPassword) {
        envValueIgnoringCase(env, "PGPASSWORD")?.let { System.setProperty("spring.datasource.password", it) }
    }
    if (dsUserProp.isNullOrBlank() && !hasExplicitUser) {
        envValueIgnoringCase(env, "PGUSER")?.let { System.setProperty("spring.datasource.username", it) }
    }
}

private fun applyDatabaseUrlCompatibility() {
    val env = System.getenv()
    // Prefer DATABASE_PUBLIC_URL (Railway public proxy) over DATABASE_URL (often internal-only) when both exist.
    val rawDatabaseUrl =
        envValueIgnoringCase(env, "DATABASE_PUBLIC_URL")
            ?: envValueIgnoringCase(env, "DATABASE_URL")

    if (!rawDatabaseUrl.isNullOrBlank()) {
        when {
            rawDatabaseUrl.startsWith("jdbc:postgresql:", ignoreCase = true) ||
                rawDatabaseUrl.startsWith("jdbc:postgres:", ignoreCase = true) -> {
                System.setProperty("spring.datasource.url", ensureRailwayPublicJdbcUrlHasSsl(rawDatabaseUrl))
            }
            else -> applyJdbcPropertiesFromUrl(rawDatabaseUrl, env)
        }
    } else {
        envValueIgnoringCase(env, "SPRING_DATASOURCE_URL")?.let {
            System.setProperty("spring.datasource.url", ensureRailwayPublicJdbcUrlHasSsl(it))
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
        envValueIgnoringCase(env, "DATABASE_URL") != null ||
            envValueIgnoringCase(env, "DATABASE_PUBLIC_URL") != null ||
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
        !envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank() ||
            !envValueIgnoringCase(env, "POSTGRES_PASSWORD").isNullOrBlank() ||
            !envValueIgnoringCase(env, "PGPASSWORD").isNullOrBlank() ||
            !envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank()
    val sysDsPassword = System.getProperty("spring.datasource.password")?.isNotBlank() == true
    val dbUrl = envValueIgnoringCase(env, "DATABASE_URL") ?: ""
    val dbUrlPublic = envValueIgnoringCase(env, "DATABASE_PUBLIC_URL") ?: ""
    if (explicitPassword ||
        sysDsPassword ||
        databaseUrlAppearsToEmbedPassword(dbUrl) ||
        databaseUrlAppearsToEmbedPassword(dbUrlPublic)
    ) {
        return
    }
    log.warn(
        "Active profile is staging/prod but no DB password was detected (DATABASE_PASSWORD / POSTGRES_PASSWORD / PGPASSWORD / SPRING_DATASOURCE_PASSWORD, or user:password inside DATABASE_PUBLIC_URL / DATABASE_URL). " +
            "On Railway, reference Postgres DATABASE_PUBLIC_URL (public) or DATABASE_URL and PGPASSWORD (or embed credentials in the URL). Hikari will usually fail with 'password authentication failed' or similar."
    )
}

private fun warnIfRailwayStagingHasNoPostgresEnv() {
    val env = System.getenv()
    if (!isRailwayDeployment(env)) {
        return
    }
    val activeRaw = System.getProperty("spring.profiles.active") ?: env["SPRING_PROFILES_ACTIVE"] ?: ""
    val stagingActive =
        activeRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.contains("staging")
    if (!stagingActive) {
        return
    }
    val hasUrl =
        envValueIgnoringCase(env, "DATABASE_PUBLIC_URL") != null ||
            envValueIgnoringCase(env, "DATABASE_URL") != null ||
            envValueIgnoringCase(env, "SPRING_DATASOURCE_URL") != null
    val hasPgHost = envValueIgnoringCase(env, "PGHOST") != null
    if (hasUrl || hasPgHost) {
        return
    }
    log.error(
        "Railway + staging: DATABASE_PUBLIC_URL, DATABASE_URL, SPRING_DATASOURCE_URL, and PGHOST are all missing from the JVM environment. " +
            "The app will fall back to the public proxy default in application-staging.yml (crossover.proxy.rlwy.net + sslmode=require). " +
            "You still need PGPASSWORD / PGUSER (or credentials inside DATABASE_PUBLIC_URL). " +
            "Best fix: backend service → Variables → **Reference** Postgres → DATABASE_PUBLIC_URL (recommended without Private Networking), plus PGPASSWORD if not in the URL, then redeploy."
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
            ?: envValueIgnoringCase(env, "SPRING_DATASOURCE_URL")
            ?: envValueIgnoringCase(env, "DATABASE_PUBLIC_URL")
            ?: envValueIgnoringCase(env, "DATABASE_URL")
            ?: run {
                val host = envValueIgnoringCase(env, "POSTGRES_HOST") ?: "localhost"
                val pgHost = envValueIgnoringCase(env, "PGHOST")
                val port =
                    envValueIgnoringCase(env, "POSTGRES_PORT")
                        ?: envValueIgnoringCase(env, "PGPORT") ?: "5432"
                val db =
                    envValueIgnoringCase(env, "POSTGRES_DB")
                        ?: envValueIgnoringCase(env, "PGDATABASE") ?: "araro_kids"
                val resolvedHost = pgHost ?: host
                if (stagingActive && host == "localhost" && pgHost.isNullOrBlank()) {
                    // application-staging.yml defaults when POSTGRES_* / PG* are unset (public Railway proxy)
                    "jdbc:postgresql://crossover.proxy.rlwy.net:58777/railway?sslmode=require"
                } else {
                    "jdbc:postgresql://$resolvedHost:$port/$db"
                }
            }
    val sanitized = url.replace(Regex("://[^/@]+@"), "://***@")
    log.info("Datasource target (pre-Spring bind, best-effort): {}", sanitized)

    val dsUrlSetAtStartup = System.getProperty("spring.datasource.url")?.isNotBlank() == true
    val hasJdbcFromDatabaseUrl =
        envValueIgnoringCase(env, "DATABASE_PUBLIC_URL") != null ||
            envValueIgnoringCase(env, "DATABASE_URL") != null ||
            envValueIgnoringCase(env, "SPRING_DATASOURCE_URL") != null
    val hasPgHostEnv = envValueIgnoringCase(env, "PGHOST") != null
    if (stagingActive && !dsUrlSetAtStartup && !hasJdbcFromDatabaseUrl && !hasPgHostEnv) {
        log.warn(
            "Staging profile: no DATABASE_PUBLIC_URL / DATABASE_URL / SPRING_DATASOURCE_URL / PGHOST visible to the JVM before Spring starts; " +
                "Spring will use application-staging.yml default (Railway public proxy crossover.proxy.rlwy.net:58777 + sslmode=require) unless you reference Postgres on the backend service."
        )
    } else if (sanitized.contains("localhost:5432") &&
        stagingActive
    ) {
        log.info(
            "Staging profile is active but datasource looks like localhost; set DATABASE_PUBLIC_URL / DATABASE_URL or Railway Postgres variables so staging does not use a local DB."
        )
    }
}

fun main(args: Array<String>) {
    val startupArgs = applyRailwayDefaultProfile(args)
    applyDatabaseUrlCompatibility()
    warnIfDatasourceEnvFamiliesOverlap()
    logLikelyDatasourceTarget()
    warnIfRailwayStagingHasNoPostgresEnv()
    warnIfLikelyMissingDbCredentials()
    runApplication<TamixaApplication>(*startupArgs)
}
