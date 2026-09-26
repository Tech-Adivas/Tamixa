package com.tamixa.infrastructure.config

import java.net.URI
import org.springframework.core.env.ConfigurableEnvironment

/**
 * When developing on a laptop (dev profile, not a Railway deployment), a copied `.env` often still contains
 * `DATABASE_PUBLIC_URL` / `DATABASE_URL` pointing at Railway's public proxy. Redirect the datasource to the team
 * team dev Postgres host (`dev.db.tamixa.in` by default, overridable with `TAMIXA_DEV_POSTGRES_HOST`) unless
 * `PGHOST` / `POSTGRES_HOST` is set to something else (use `localhost` for Docker-only). Opt out of redirect with
 * `TAMIXA_USE_REMOTE_DATABASE=true`.
 *
 * When this redirect runs, `PGPASSWORD` is intentionally **not** used for the datasource password. Use
 * `DATABASE_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` for `dev.db.tamixa.in`, or `POSTGRES_PASSWORD` when the
 * resolved JDBC host is loopback only.
 */
internal object LocalDevPostgresPreference {

    const val USE_REMOTE_DATABASE_ENV = "TAMIXA_USE_REMOTE_DATABASE"

    /** Default host when `PGHOST` / `POSTGRES_HOST` is unset or `*.railway.internal` (laptop cannot reach it). */
    const val DEFAULT_TEAM_DEV_POSTGRES_HOST = "dev.db.tamixa.in"

    /** Override [DEFAULT_TEAM_DEV_POSTGRES_HOST], e.g. `localhost` for Docker Postgres on your machine. */
    const val DEV_POSTGRES_HOST_ENV = "TAMIXA_DEV_POSTGRES_HOST"

    fun isRailwayDeploymentEnv(env: Map<*, *>): Boolean =
        env.entries.any { (key, value) ->
            key.toString().startsWith("RAILWAY_") && value?.toString()?.isNotBlank() == true
        }

    fun forceRemoteDatabase(env: Map<*, *>): Boolean =
        env.entries
            .firstOrNull { it.key.toString().equals(USE_REMOTE_DATABASE_ENV, ignoreCase = true) }
            ?.value
            ?.toString()
            ?.equals("true", ignoreCase = true) == true

    fun looksLikeRailwayPublicPostgresEndpoint(rawUrl: String): Boolean {
        val host = jdbcOrLibpqUrlHost(rawUrl) ?: return false
        return isRailwayPublicPostgresNetworkHost(host)
    }

    fun shouldPreferLocalOverRailwayPublicUrlPreSpring(env: Map<String, String>, args: Array<String>): Boolean {
        if (isRailwayDeploymentEnv(env)) return false
        if (forceRemoteDatabase(env)) return false
        val raw = resolveActiveProfilesRaw(env, args)
        val profiles =
            raw?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        if (profiles.isEmpty()) {
            return true
        }
        return devOnlyProfiles(profiles)
    }

    fun shouldPreferLocalOverRailwayPublicInSpring(environment: ConfigurableEnvironment): Boolean {
        val sysEnv = environment.systemEnvironment
        if (isRailwayDeploymentEnv(sysEnv)) return false
        if (forceRemoteDatabase(sysEnv)) return false

        var profiles =
            environment.activeProfiles.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        if (profiles.isEmpty()) {
            val prop = environment.getProperty("spring.profiles.active").orEmpty()
            profiles = prop.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        }
        if (profiles.isEmpty()) {
            return true
        }
        return devOnlyProfiles(profiles)
    }

    fun buildLocalJdbcUrlFromEnvMap(env: Map<String, String>): String {
        val host = effectiveLocalJdbcHostFromEnvMap(env)
        val port =
            envValueIgnoringCase(env, "POSTGRES_PORT")
                ?: envValueIgnoringCase(env, "PGPORT")
                ?: "5432"
        val db =
            envValueIgnoringCase(env, "POSTGRES_DB")
                ?: envValueIgnoringCase(env, "PGDATABASE")
                ?: "araro_kids"
        return jdbcUrlWithOptionalSsl(host, port, db)
    }

    /**
     * Resolves username for the local-DB redirect; same precedence as dev YAML Spring keys, then Docker-style POSTGRES_* / PG*.
     */
    fun localDevDatasourceUsernameFromEnvMap(env: Map<String, String>): String =
        envValueIgnoringCase(env, "SPRING_DATASOURCE_USERNAME")?.takeIf { it.isNotBlank() }
            ?: envValueIgnoringCase(env, "DATABASE_USERNAME")?.takeIf { it.isNotBlank() }
            ?: envValueIgnoringCase(env, "POSTGRES_USER")?.takeIf { it.isNotBlank() }
            ?: envValueIgnoringCase(env, "PGUSER")?.takeIf { it.isNotBlank() }
            ?: "postgres"

    /**
     * Password for local redirect: explicit Spring/DB env only — **not** `PGPASSWORD` (usually Railway when URL was remote).
     */
    fun localDevDatasourcePasswordFromEnvMapIgnoringPgp(env: Map<String, String>): String {
        envValueIgnoringCase(env, "SPRING_DATASOURCE_PASSWORD")?.takeIf { it.isNotBlank() }?.let { return it }
        envValueIgnoringCase(env, "DATABASE_PASSWORD")?.takeIf { it.isNotBlank() }?.let { return it }
        if (resolvedJdbcHostIsLoopbackFromEnvMap(env)) {
            envValueIgnoringCase(env, "POSTGRES_PASSWORD")?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return "postgres"
    }

    fun localDevDatasourceUsernameSpring(environment: ConfigurableEnvironment): String =
        environment.getProperty("SPRING_DATASOURCE_USERNAME")?.takeIf { it.isNotBlank() }
            ?: environment.getProperty("DATABASE_USERNAME")?.takeIf { it.isNotBlank() }
            ?: environment.getProperty("POSTGRES_USER")?.takeIf { it.isNotBlank() }
            ?: environment.getProperty("PGUSER")?.takeIf { it.isNotBlank() }
            ?: "postgres"

    fun localDevDatasourcePasswordSpringIgnoringPgp(environment: ConfigurableEnvironment): String {
        environment.getProperty("SPRING_DATASOURCE_PASSWORD")?.takeIf { it.isNotBlank() }?.let { return it }
        environment.getProperty("DATABASE_PASSWORD")?.takeIf { it.isNotBlank() }?.let { return it }
        if (resolvedJdbcHostIsLoopbackSpring(environment)) {
            environment.getProperty("POSTGRES_PASSWORD")?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return "postgres"
    }

    fun buildLocalJdbcUrl(environment: ConfigurableEnvironment): String {
        val host = effectiveLocalJdbcHostSpring(environment)
        val port =
            environment.getProperty("POSTGRES_PORT")?.takeIf { it.isNotBlank() }
                ?: environment.getProperty("PGPORT")?.takeIf { it.isNotBlank() }
                ?: "5432"
        val db =
            environment.getProperty("POSTGRES_DB")?.takeIf { it.isNotBlank() }
                ?: environment.getProperty("PGDATABASE")?.takeIf { it.isNotBlank() }
                ?: "araro_kids"
        return jdbcUrlWithOptionalSsl(host, port, db)
    }

    private fun devOnlyProfiles(profiles: Set<String>): Boolean {
        if (!profiles.contains("dev")) return false
        if (profiles.contains("staging") || profiles.contains("prod")) return false
        return true
    }

    private fun resolveActiveProfilesRaw(env: Map<String, String>, args: Array<String>): String? {
        envValueIgnoringCase(env, "SPRING_PROFILES_ACTIVE")?.takeIf { it.isNotBlank() }?.let { return it }
        System.getProperty("spring.profiles.active")?.takeIf { it.isNotBlank() }?.let { return it }
        var i = 0
        while (i < args.size) {
            val a = args[i]
            when {
                a.startsWith("--spring.profiles.active=") -> return a.substringAfter("=", "")
                a == "--spring.profiles.active" && i + 1 < args.size -> return args[i + 1]
            }
            i++
        }
        return null
    }

    private fun jdbcOrLibpqUrlHost(raw: String): String? {
        val pseudo =
            when {
                raw.trim().startsWith("jdbc:", ignoreCase = true) ->
                    raw.trim().replaceFirst(Regex("^jdbc:", RegexOption.IGNORE_CASE), "")
                raw.trim().startsWith("postgresql://", ignoreCase = true) ||
                    raw.trim().startsWith("postgres://", ignoreCase = true) -> raw.trim()
                else -> return null
            }
        return try {
            URI(pseudo).host
        } catch (_: Exception) {
            null
        }
    }

    private fun isRailwayPublicPostgresNetworkHost(host: String): Boolean =
        host.contains("proxy.rlwy.net", ignoreCase = true) ||
            (host.endsWith(".rlwy.net", ignoreCase = true) && !host.contains("railway.internal", ignoreCase = true))

    private fun envValueIgnoringCase(env: Map<String, String>, key: String): String? {
        env[key]?.takeIf { it.isNotBlank() }?.let { return it }
        return env.entries.firstOrNull { it.key.equals(key, ignoreCase = true) && it.value.isNotBlank() }?.value
    }

    private fun defaultTeamDevPostgresHostFromEnvMap(env: Map<String, String>): String =
        envValueIgnoringCase(env, DEV_POSTGRES_HOST_ENV)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_TEAM_DEV_POSTGRES_HOST

    private fun defaultTeamDevPostgresHostSpring(environment: ConfigurableEnvironment): String =
        environment.getProperty(DEV_POSTGRES_HOST_ENV)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_TEAM_DEV_POSTGRES_HOST

    /**
     * `railway.env` often sets `PGHOST=postgres.railway.internal`; laptops cannot use that, so fall back to the team
     * dev host (or [TAMIXA_DEV_POSTGRES_HOST]).
     */
    private fun effectiveLocalJdbcHostFromEnvMap(env: Map<String, String>): String {
        val raw =
            envValueIgnoringCase(env, "POSTGRES_HOST")
                ?: envValueIgnoringCase(env, "PGHOST")
                ?: return defaultTeamDevPostgresHostFromEnvMap(env)
        if (raw.contains("railway.internal", ignoreCase = true)) {
            return defaultTeamDevPostgresHostFromEnvMap(env)
        }
        return raw
    }

    private fun resolvedJdbcHostIsLoopbackFromEnvMap(env: Map<String, String>): Boolean {
        val h = effectiveLocalJdbcHostFromEnvMap(env)
        return h.equals("localhost", ignoreCase = true) || h == "127.0.0.1"
    }

    private fun effectiveLocalJdbcHostSpring(environment: ConfigurableEnvironment): String {
        val raw =
            environment.getProperty("POSTGRES_HOST")?.takeIf { it.isNotBlank() }
                ?: environment.getProperty("PGHOST")?.takeIf { it.isNotBlank() }
                ?: return defaultTeamDevPostgresHostSpring(environment)
        if (raw.contains("railway.internal", ignoreCase = true)) {
            return defaultTeamDevPostgresHostSpring(environment)
        }
        return raw
    }

    private fun resolvedJdbcHostIsLoopbackSpring(environment: ConfigurableEnvironment): Boolean {
        val h = effectiveLocalJdbcHostSpring(environment)
        return h.equals("localhost", ignoreCase = true) || h == "127.0.0.1"
    }

    private fun jdbcUrlWithOptionalSsl(host: String, port: String, db: String): String {
        val base = "jdbc:postgresql://$host:$port/$db"
        if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1") {
            return base
        }
        return "$base?sslmode=require"
    }
}
