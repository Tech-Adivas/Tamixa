package com.tamixa

import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Explicit JDBC connectivity against the Testcontainers Postgres used by [IntegrationTestBase].
 * Complements [TamixaApplicationTests] (context load) with a cheap `SELECT 1` and [Connection.isValid].
 */
class DatabaseConnectivityIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `datasource connection is valid`() {
        dataSource.connection.use { conn ->
            assertThat(conn.isValid(5))
                .withFailMessage("JDBC connection should be valid within 5s")
                .isTrue()
        }
    }

    @Test
    fun `datasource executes select one`() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT 1 AS ok").use { rs ->
                    assertThat(rs.next()).isTrue()
                    assertThat(rs.getInt("ok")).isEqualTo(1)
                }
            }
        }
    }
}
