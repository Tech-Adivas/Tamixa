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
import kotlin.system.exitProcess

@SpringBootApplication
@EnableJpaRepositories
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
    // Railway / Docker often expose PG*; some templates only set POSTGRES_HOST on the consuming service.
    val pgHost =
        envValueIgnoringCase(env, "PGHOST")
            ?: envValueIgnoringCase(env, "POSTGRES_HOST")
            ?: return
    val pgPort =
        envValueIgnoringCase(env, "PGPORT")
            ?: envValueIgnoringCase(env, "POSTGRES_PORT")
            ?: "5432"
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
        val user = envValueIgnoringCase(env, "PGUSER") ?: envValueIgnoringCase(env, "POSTGRES_USER")
        user?.takeIf { it.isNotBlank() }?.let { System.setProperty("spring.datasource.username", it) }
    }
    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank()
    ) {
        val pw = envValueIgnoringCase(env, "PGPASSWORD") ?: envValueIgnoringCase(env, "POSTGRES_PASSWORD")
        pw?.takeIf { it.isNotBlank() }?.let { System.setProperty("spring.datasource.password", it) }
    }
    log.info("Configured datasource from PG* / POSTGRES_* host environment variables")
}

/**
 * After we set `spring.datasource.url`, copy PG* / POSTGRES_* into Spring properties when Spring-specific env is unset
 * (JDBC URLs without embedded credentials need a separate password env — reference `PGPASSWORD` or `POSTGRES_PASSWORD`).
 */
private fun applyDatasourceSecretsFromRailwayPgEnv(env: Map<String, String>) {
    val dsPassProp = System.getProperty("spring.datasource.password")?.takeIf { it.isNotBlank() }
    val dsUserProp = System.getProperty("spring.datasource.username")?.takeIf { it.isNotBlank() }
    val hasSpringOrDatabasePassword =
        !envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank() ||
            !envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank()
    val hasSpringOrDatabaseUser =
        !envValueIgnoringCase(env, "SPRING_DATASOURCE_USERNAME").isNullOrBlank() ||
            !envValueIgnoringCase(env, "DATABASE_USERNAME").isNullOrBlank()
    if (dsPassProp.isNullOrBlank() && !hasSpringOrDatabasePassword) {
        val pw = envValueIgnoringCase(env, "PGPASSWORD") ?: envValueIgnoringCase(env, "POSTGRES_PASSWORD")
        pw?.takeIf { it.isNotBlank() }?.let { System.setProperty("spring.datasource.password", it) }
    }
    if (dsUserProp.isNullOrBlank() && !hasSpringOrDatabaseUser) {
        val user = envValueIgnoringCase(env, "PGUSER") ?: envValueIgnoringCase(env, "POSTGRES_USER")
        user?.takeIf { it.isNotBlank() }?.let { System.setProperty("spring.datasource.username", it) }
    }
}

private fun applyDatabaseUrlCompatibility() {
    val env = System.getenv()
    // URL choice when both Railway URLs exist:
    // - On Railway (RAILWAY_*): prefer private DATABASE_URL (postgres.railway.internal) — same DB, lower latency, no public proxy hop.
    // - Off Railway (e.g. laptop with railway.env): prefer DATABASE_PUBLIC_URL so internal hostnames are not tried first.
    val rawDatabaseUrl =
        if (isRailwayDeployment(env)) {
            envValueIgnoringCase(env, "DATABASE_URL")
                ?: envValueIgnoringCase(env, "DATABASE_PUBLIC_URL")
        } else {
            envValueIgnoringCase(env, "DATABASE_PUBLIC_URL")
                ?: envValueIgnoringCase(env, "DATABASE_URL")
        }

    if (!rawDatabaseUrl.isNullOrBlank()) {
        when {
            rawDatabaseUrl.startsWith("jdbc:postgresql:", ignoreCase = true) ||
                rawDatabaseUrl.startsWith("jdbc:postgres:", ignoreCase = true) -> {
                val jdbcUrl = ensureRailwayPublicJdbcUrlHasSsl(rawDatabaseUrl)
                System.setProperty("spring.datasource.url", jdbcUrl)
                applyEmbeddedCredentialsFromPostgresStyleUrl(jdbcUrl, env)
            }
            else -> applyJdbcPropertiesFromUrl(rawDatabaseUrl, env)
        }
    } else {
        envValueIgnoringCase(env, "SPRING_DATASOURCE_URL")?.let {
            val jdbcUrl = ensureRailwayPublicJdbcUrlHasSsl(it)
            System.setProperty("spring.datasource.url", jdbcUrl)
            applyEmbeddedCredentialsFromPostgresStyleUrl(jdbcUrl, env)
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

/** postgresql://, postgres://, or jdbc:postgresql:// — for credential checks and optional extraction. */
private fun toUriStylePostgresUrl(raw: String): String? {
    val t = raw.trim()
    return when {
        t.startsWith("jdbc:postgresql://", ignoreCase = true) ||
            t.startsWith("jdbc:postgres://", ignoreCase = true) ->
            t.replaceFirst(Regex("^jdbc:", RegexOption.IGNORE_CASE), "")
        t.startsWith("postgresql://", ignoreCase = true) ||
            t.startsWith("postgres://", ignoreCase = true) -> t
        else -> null
    }
}

/**
 * True if the URL appears to carry a non-blank password (user:password@host).
 * URI parsing can fail on rare unescaped characters; a small regex fallback avoids false "missing password" warnings.
 */
private fun postgresConnectionUrlEmbedsPassword(raw: String): Boolean {
    val uriStyle = toUriStylePostgresUrl(raw) ?: return false
    val viaUri =
        try {
            val uri = URI(uriStyle)
            val ui = uri.userInfo
            ui != null && ui.contains(":") && ui.substringAfter(":").isNotBlank()
        } catch (_: Exception) {
            false
        }
    if (viaUri) {
        return true
    }
    return Regex("://[^/@]+:[^@]+@", RegexOption.IGNORE_CASE).containsMatchIn(uriStyle)
}

/**
 * When a JDBC URL still contains user:password@, copy them into Spring datasource properties if not already set.
 * (Spring Boot often expects separate username/password; Hikari may not always infer them from the JDBC URL alone.)
 */
private fun applyEmbeddedCredentialsFromPostgresStyleUrl(rawUrl: String, env: Map<String, String>) {
    val uriStyle = toUriStylePostgresUrl(rawUrl) ?: return
    val userInfo: String? =
        try {
            URI(uriStyle).userInfo?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            Regex("://([^@]+)@", RegexOption.IGNORE_CASE).find(uriStyle)?.groupValues?.getOrNull(1)
        }
    if (userInfo.isNullOrBlank() || !userInfo.contains(":")) {
        return
    }
    val username = decodeUserInfoPart(userInfo.substringBefore(':'))
    val password = decodeUserInfoPart(userInfo.substringAfter(':', missingDelimiterValue = ""))
    if (username.isBlank() || password.isBlank()) {
        return
    }
    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_USERNAME").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_USERNAME").isNullOrBlank() &&
        System.getProperty("spring.datasource.username").isNullOrBlank()
    ) {
        System.setProperty("spring.datasource.username", username)
    }
    if (envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD").isNullOrBlank() &&
        envValueIgnoringCase(env, "DATABASE_PASSWORD").isNullOrBlank() &&
        System.getProperty("spring.datasource.password").isNullOrBlank()
    ) {
        System.setProperty("spring.datasource.password", password)
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
    val springDsUrl = envValueIgnoringCase(env, "SPRING_DATASOURCE_URL") ?: ""
    val normalizedJdbc = System.getProperty("spring.datasource.url") ?: ""
    if (explicitPassword ||
        sysDsPassword ||
        postgresConnectionUrlEmbedsPassword(dbUrlPublic) ||
        postgresConnectionUrlEmbedsPassword(dbUrl) ||
        postgresConnectionUrlEmbedsPassword(springDsUrl) ||
        postgresConnectionUrlEmbedsPassword(normalizedJdbc)
    ) {
        return
    }
    log.warn(
        "Active profile is staging/prod but no DB password was detected (DATABASE_PASSWORD / POSTGRES_PASSWORD / PGPASSWORD / SPRING_DATASOURCE_PASSWORD, or user:password inside DATABASE_* URLs / SPRING_DATASOURCE_URL). " +
            "Private networking: reference DATABASE_URL (omit DATABASE_PUBLIC_URL on the service if you want private only) and PGPASSWORD or POSTGRES_PASSWORD when the connection URL has no embedded credentials. Hikari will usually fail with 'password authentication failed' or similar."
    )
}

/**
 * On Railway, **staging** must receive Postgres connection variables on **the same service that runs this JVM**
 * (Variable Reference from the Postgres plugin — even in a single-service / monorepo deploy).
 * Without them, Spring used to use a placeholder JDBC URL with no password (SCRAM / NPE noise). **Prod** is not halted here:
 * `application-prod.yml` can build a private `jdbc:postgresql://postgres.railway.internal/...` URL with only `PGPASSWORD` set.
 */
private fun haltIfRailwayMissingPostgresConnectionEnv() {
    val env = System.getenv()
    if (!isRailwayDeployment(env)) {
        return
    }
    val activeRaw = System.getProperty("spring.profiles.active") ?: env["SPRING_PROFILES_ACTIVE"] ?: ""
    val profiles = activeRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
    if (!profiles.contains("staging")) {
        return
    }
    val hasConnectionHint =
        envValueIgnoringCase(env, "DATABASE_PUBLIC_URL") != null ||
            envValueIgnoringCase(env, "DATABASE_URL") != null ||
            envValueIgnoringCase(env, "SPRING_DATASOURCE_URL") != null ||
            envValueIgnoringCase(env, "PGHOST") != null ||
            !envValueIgnoringCase(env, "POSTGRES_HOST").isNullOrBlank()
    if (hasConnectionHint) {
        return
    }
    log.error(
        "Railway: Postgres env is not visible to this JVM (needs DATABASE_URL, DATABASE_PUBLIC_URL, SPRING_DATASOURCE_URL, " +
            "or PGHOST/POSTGRES_HOST with PGDATABASE/POSTGRES_DB). Railway does not auto-inject DB vars: open the service that runs " +
            "this container → Variables → New Variable → Variable Reference → pick your **Postgres** service → add DATABASE_URL " +
            "(and DATABASE_PUBLIC_URL if you use it), plus PGPASSWORD/POSTGRES_PASSWORD if the URL has no password. " +
            "Raw-editor equivalent looks like: DATABASE_URL=\${{ Postgres.DATABASE_URL }} (use your actual Postgres service name). " +
            "Save and redeploy."
    )
    exitProcess(1)
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
                    "(staging: configure DATABASE_PUBLIC_URL or DATABASE_URL — no default proxy in application-staging.yml)"
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
    val hasPostgresHostEnv = !envValueIgnoringCase(env, "POSTGRES_HOST").isNullOrBlank()
    val hasPgHostEnv = envValueIgnoringCase(env, "PGHOST") != null
    if (stagingActive && !dsUrlSetAtStartup && !hasJdbcFromDatabaseUrl && !hasPgHostEnv && !hasPostgresHostEnv) {
        log.warn(
            "Staging profile: no DATABASE_PUBLIC_URL / DATABASE_URL / SPRING_DATASOURCE_URL / PGHOST / POSTGRES_HOST visible to the JVM before Spring starts; " +
                "set spring.datasource.url via env (reference Postgres on Railway) or the app will not connect."
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
    haltIfRailwayMissingPostgresConnectionEnv()
    warnIfLikelyMissingDbCredentials()
    runApplication<TamixaApplication>(*startupArgs)
}
