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

private fun applyDatabaseUrlCompatibility() {
    val env = System.getenv()
    val springDatasourceUrlEnv = env["SPRING_DATASOURCE_URL"]?.takeIf { it.isNotBlank() }
    if (!springDatasourceUrlEnv.isNullOrBlank()) {
        return
    }

    val rawDatabaseUrl = env["DATABASE_URL"]?.takeIf { it.isNotBlank() } ?: return
    val looksLikeRailwayStyleUrl =
        rawDatabaseUrl.startsWith("postgresql://", ignoreCase = true) ||
            rawDatabaseUrl.startsWith("postgres://", ignoreCase = true)
    if (!looksLikeRailwayStyleUrl) {
        return
    }

    val uri =
        try {
            URI(rawDatabaseUrl)
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

private fun logLikelyDatasourceTarget() {
    val env = System.getenv()
    val url =
        System.getProperty("spring.datasource.url")?.takeIf { it.isNotBlank() }
            ?: env["SPRING_DATASOURCE_URL"]?.takeIf { it.isNotBlank() }
            ?: env["DATABASE_URL"]?.takeIf { it.isNotBlank() }
            ?: run {
                val host = env["POSTGRES_HOST"]?.takeIf { it.isNotBlank() } ?: "localhost"
                val port = env["POSTGRES_PORT"]?.takeIf { it.isNotBlank() } ?: "5432"
                val db = env["POSTGRES_DB"]?.takeIf { it.isNotBlank() } ?: "araro_kids"
                "jdbc:postgresql://$host:$port/$db"
            }
    val sanitized = url.replace(Regex("://[^/@]+@"), "://***@")
    log.info("Datasource target (resolved from env): {}", sanitized)
}

fun main(args: Array<String>) {
    applyDatabaseUrlCompatibility()
    warnIfDatasourceEnvFamiliesOverlap()
    logLikelyDatasourceTarget()
    runApplication<TamixaApplication>(*args)
}
