package com.tamixa

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
        env["DATABASE_URL"]?.takeIf { it.isNotBlank() }
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
    warnIfDatasourceEnvFamiliesOverlap()
    logLikelyDatasourceTarget()
    runApplication<TamixaApplication>(*args)
}
