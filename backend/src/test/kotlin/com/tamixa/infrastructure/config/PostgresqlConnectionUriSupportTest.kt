package com.tamixa.infrastructure.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PostgresqlConnectionUriSupportTest {

    @Test
    fun normalize_leaves_jdbc_unchanged() {
        val u = "jdbc:postgresql://db.example.com:5432/mydb?sslmode=require"
        assertEquals(u, PostgresqlConnectionUriSupport.normalizeToJdbcUrl(u))
    }

    @Test
    fun normalize_libpq_to_jdbc() {
        val raw = "postgresql://postgres:secret@interchange.proxy.rlwy.net:44627/railway"
        assertEquals(
            "jdbc:postgresql://interchange.proxy.rlwy.net:44627/railway",
            PostgresqlConnectionUriSupport.normalizeToJdbcUrl(raw),
        )
    }

    @Test
    fun parse_extracts_credentials() {
        val p = PostgresqlConnectionUriSupport.parseLibpqStyleUri(
            "postgresql://user:p%40ss@host:5432/dbname",
        )!!
        assertEquals("jdbc:postgresql://host:5432/dbname", p.jdbcUrl)
        assertEquals("user", p.username)
        assertEquals("p@ss", p.password)
    }

    @Test
    fun parse_rejects_non_postgres_scheme() {
        assertNull(PostgresqlConnectionUriSupport.parseLibpqStyleUri("http://localhost/db"))
    }
}
