package com.tamixa.infrastructure.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalDevPostgresPreferenceTest {

    @Test
    fun railway_public_proxy_hosts_are_detected() {
        assertTrue(
            LocalDevPostgresPreference.looksLikeRailwayPublicPostgresEndpoint(
                "postgresql://u:p@interchange.proxy.rlwy.net:44627/railway",
            ),
        )
        assertTrue(
            LocalDevPostgresPreference.looksLikeRailwayPublicPostgresEndpoint(
                "jdbc:postgresql://interchange.proxy.rlwy.net:44627/railway?sslmode=require",
            ),
        )
    }

    @Test
    fun railway_internal_and_localhost_are_not_public_proxy() {
        assertFalse(
            LocalDevPostgresPreference.looksLikeRailwayPublicPostgresEndpoint(
                "jdbc:postgresql://postgres.railway.internal:5432/railway",
            ),
        )
        assertFalse(
            LocalDevPostgresPreference.looksLikeRailwayPublicPostgresEndpoint(
                "jdbc:postgresql://localhost:5432/araro_kids",
            ),
        )
    }

    @Test
    fun buildLocalJdbcUrlFromEnvMap_matches_dev_defaults() {
        assertEquals(
            "jdbc:postgresql://dev.db.tamixa.in:5432/araro_kids?sslmode=require",
            LocalDevPostgresPreference.buildLocalJdbcUrlFromEnvMap(emptyMap()),
        )
        assertEquals(
            "jdbc:postgresql://localhost:5432/araro_kids",
            LocalDevPostgresPreference.buildLocalJdbcUrlFromEnvMap(
                mapOf(LocalDevPostgresPreference.DEV_POSTGRES_HOST_ENV to "localhost"),
            ),
        )
        val env =
            mapOf(
                "PGHOST" to "127.0.0.1",
                "PGPORT" to "5433",
                "PGDATABASE" to "mydb",
            )
        assertEquals(
            "jdbc:postgresql://127.0.0.1:5433/mydb",
            LocalDevPostgresPreference.buildLocalJdbcUrlFromEnvMap(env),
        )
    }

    @Test
    fun railway_deployment_env_detected() {
        assertTrue(LocalDevPostgresPreference.isRailwayDeploymentEnv(mapOf("RAILWAY_ENVIRONMENT" to "production")))
        assertFalse(LocalDevPostgresPreference.isRailwayDeploymentEnv(mapOf("HOME" to "/tmp")))
    }

    @Test
    fun local_password_ignores_pgpassword() {
        val env =
            mapOf(
                "PGPASSWORD" to "railway-secret",
                "POSTGRES_USER" to "postgres",
            )
        assertEquals("postgres", LocalDevPostgresPreference.localDevDatasourcePasswordFromEnvMapIgnoringPgp(env))
        val railwayStyle =
            env +
                mapOf(
                    "POSTGRES_PASSWORD" to "railway-secret",
                    "PGHOST" to "postgres.railway.internal",
                )
        assertEquals("postgres", LocalDevPostgresPreference.localDevDatasourcePasswordFromEnvMapIgnoringPgp(railwayStyle))
        assertEquals(
            "localpw",
            LocalDevPostgresPreference.localDevDatasourcePasswordFromEnvMapIgnoringPgp(
                mapOf(
                    "PGHOST" to "localhost",
                    "POSTGRES_PASSWORD" to "localpw",
                ),
            ),
        )
    }

    @Test
    fun local_jdbc_url_replaces_railway_internal_host_with_team_dev() {
        val env = mapOf("PGHOST" to "postgres.railway.internal", "PGPORT" to "5432", "PGDATABASE" to "railway")
        assertEquals(
            "jdbc:postgresql://dev.db.tamixa.in:5432/railway?sslmode=require",
            LocalDevPostgresPreference.buildLocalJdbcUrlFromEnvMap(env),
        )
    }

    @Test
    fun local_username_prefers_postgres_family_over_pguser() {
        val env = mapOf("PGUSER" to "postgres", "POSTGRES_USER" to "appuser")
        assertEquals("appuser", LocalDevPostgresPreference.localDevDatasourceUsernameFromEnvMap(env))
    }
}
