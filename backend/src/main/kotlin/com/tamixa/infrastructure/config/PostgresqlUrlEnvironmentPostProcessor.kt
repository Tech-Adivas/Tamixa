package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Unifies PostgreSQL configuration across profiles:
 *
 * 1. If `DATABASE_URL` is set but missing the `jdbc:` prefix (common copy-paste from libpq URLs), normalize it.
 * 2. If `DATABASE_URL` is blank but `DATABASE_PUBLIC_URL` is set (e.g. Railway), derive `spring.datasource.url`
 *    and optionally username/password when those are not already set via env/yaml.
 *
 * Runs late so `application-{profile}.yml` placeholders are already applied; overrides are added with highest precedence.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class PostgresqlUrlEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val databaseUrlEnv = environment.getProperty("DATABASE_URL")?.trim().orEmpty()
        val publicUrlEnv = environment.getProperty("DATABASE_PUBLIC_URL")?.trim().orEmpty()

        val updates = linkedMapOf<String, Any>()

        when {
            databaseUrlEnv.isNotEmpty() -> {
                val normalized = PostgresqlConnectionUriSupport.normalizeToJdbcUrl(databaseUrlEnv)
                if (normalized != null) {
                    val resolvedFromYaml = environment.getProperty("spring.datasource.url")
                    if (normalized != resolvedFromYaml) {
                        updates["spring.datasource.url"] = normalized
                        if (!databaseUrlEnv.startsWith("jdbc:", ignoreCase = true)) {
                            log.info("Normalized DATABASE_URL to JDBC form for Spring DataSource binding")
                        }
                    }
                }
                val parsed = PostgresqlConnectionUriSupport.parseLibpqStyleUri(databaseUrlEnv)
                if (parsed != null && hasNoDatasourceCredentials(environment)) {
                    parsed.username?.let { updates["spring.datasource.username"] = it }
                    parsed.password?.let { updates["spring.datasource.password"] = it }
                }
            }
            publicUrlEnv.isNotEmpty() -> {
                if (hasPostgresHostFromEnv(environment)) {
                    // Docker Compose / Railway private networking: POSTGRES_HOST or PGHOST builds JDBC from yaml — do not override with public proxy URL.
                    return
                }
                val parsed = PostgresqlConnectionUriSupport.parseLibpqStyleUri(publicUrlEnv) ?: run {
                    log.warn("DATABASE_PUBLIC_URL is set but could not be parsed as a postgresql:// URI; ignoring")
                    return
                }
                updates["spring.datasource.url"] = parsed.jdbcUrl
                if (hasNoDatasourceCredentials(environment)) {
                    parsed.username?.let { updates["spring.datasource.username"] = it }
                    parsed.password?.let { updates["spring.datasource.password"] = it }
                }
                log.info("Applied DATABASE_PUBLIC_URL to spring.datasource.url")
            }
        }

        if (updates.isNotEmpty()) {
            environment.propertySources.addFirst(
                MapPropertySource("tamixaPostgresqlUrlOverrides", updates),
            )
        }
    }

    private fun hasPostgresHostFromEnv(environment: ConfigurableEnvironment): Boolean {
        val h = environment.getProperty("POSTGRES_HOST")?.trim().orEmpty()
        val g = environment.getProperty("PGHOST")?.trim().orEmpty()
        return h.isNotEmpty() || g.isNotEmpty()
    }

    private fun hasNoDatasourceCredentials(environment: ConfigurableEnvironment): Boolean {
        fun blank(key: String) = environment.getProperty(key).isNullOrBlank()
        return blank("spring.datasource.username") &&
            blank("DATABASE_USERNAME") &&
            blank("POSTGRES_USER") &&
            blank("PGUSER") &&
            blank("spring.datasource.password") &&
            blank("DATABASE_PASSWORD") &&
            blank("POSTGRES_PASSWORD") &&
            blank("PGPASSWORD")
    }
}
