package com.tamixa.infrastructure.config

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Parses `postgresql://` / `postgres://` connection strings (e.g. Railway `DATABASE_PUBLIC_URL`)
 * into a JDBC URL without credentials plus optional username/password.
 */
internal object PostgresqlConnectionUriSupport {

    data class Parsed(
        val jdbcUrl: String,
        val username: String?,
        val password: String?,
    )

    /**
     * Ensures a value is suitable for [org.springframework.boot.jdbc.autoconfigure.DataSourceProperties].
     * Accepts `jdbc:postgresql://...` or libpq-style `postgresql://...` / `postgres://...`.
     */
    fun normalizeToJdbcUrl(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (t.startsWith("jdbc:", ignoreCase = true)) return t
        if (t.startsWith("postgresql://", ignoreCase = true) || t.startsWith("postgres://", ignoreCase = true)) {
            return parseLibpqStyleUri(t)?.jdbcUrl
        }
        return null
    }

    fun parseLibpqStyleUri(uriString: String): Parsed? {
        val uri = try {
            URI(uriString.trim())
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "postgresql" && scheme != "postgres") return null
        val host = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 5432
        val path = uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() } ?: "postgres"
        val query = uri.rawQuery?.let { "?$it" } ?: ""

        val userInfo = uri.userInfo
        var username: String? = null
        var password: String? = null
        if (!userInfo.isNullOrBlank()) {
            val idx = userInfo.indexOf(':')
            if (idx >= 0) {
                username = urlDecode(userInfo.substring(0, idx))
                password = urlDecode(userInfo.substring(idx + 1))
            } else {
                username = urlDecode(userInfo)
            }
        }

        val jdbcUrl = "jdbc:postgresql://$host:$port/$path$query"
        return Parsed(jdbcUrl = jdbcUrl, username = username, password = password)
    }

    private fun urlDecode(s: String): String =
        URLDecoder.decode(s, StandardCharsets.UTF_8)
}
