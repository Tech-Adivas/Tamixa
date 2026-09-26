package com.tamixa.network

object ApiConfig {
    const val API_VERSION = "/api/v1"
    /** Android emulator: 10.0.2.2 = host localhost. iOS Simulator: use localhost. Physical device: use machine IP. */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

    private val ABS_HTTP_URL = Regex("^https?://([^/?#]+)(.*)$", RegexOption.IGNORE_CASE)

    private fun rewriteLocalhostAliasesToBase(absoluteUrl: String, base: String): String {
        if (!absoluteUrl.contains("localhost") && !absoluteUrl.contains("127.0.0.1") && !absoluteUrl.contains("10.0.2.2")) {
            return absoluteUrl
        }
        return absoluteUrl
            .replace(Regex("https?://localhost(?::\\d+)?"), base)
            .replace(Regex("https?://127\\.0\\.0\\.1(?::\\d+)?"), base)
            .replace(Regex("https?://10\\.0\\.2\\.2(?::\\d+)?"), base)
    }

    private fun effectivePort(scheme: String, authority: String): Int {
        val afterColon = authority.substringAfter(':', missingDelimiterValue = "")
        if (afterColon.isNotEmpty() && afterColon.all { it.isDigit() }) return afterColon.toInt()
        return if (scheme == "https") 443 else 80
    }

    private fun hostOfAuthority(authority: String): String = authority.substringBefore(':')

    /** True for RFC1918 / loopback-style dev hosts (IPv4 only). */
    private fun isPrivateOrLoopbackIpv4Host(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        val octets = host.split('.')
        if (octets.size != 4) return false
        val o = octets.map { it.toIntOrNull() ?: return false }
        if (o.any { it !in 0..255 }) return false
        val a = o[0]
        if (a == 10) return true
        if (a == 127) return true
        if (a == 172 && o[1] in 16..31) return true
        if (a == 192 && o[1] == 168) return true
        return false
    }

    private fun pathOnly(rest: String): String = rest.substringBefore('?').substringBefore('#').ifEmpty { "/" }

    private fun isBackendAudioOrApiPath(rest: String): Boolean {
        val p = pathOnly(rest)
        return p == "/audio" || p.startsWith("/audio/") || p == "/api" || p.startsWith("/api/")
    }

    private fun isBackendCoverPath(rest: String): Boolean {
        val p = pathOnly(rest)
        return p == "/covers" || p.startsWith("/covers/")
    }

    /**
     * Backend may embed the machine LAN IP (e.g. 192.168.x.x) in stored URLs while the app uses
     * [apiBaseUrl] (e.g. 10.0.2.2 on Android emulator). ExoPlayer would then fail to connect.
     * Rewrites same-port private/LAN absolute URLs that point at our HTTP paths to use [apiBaseUrl]'s origin.
     */
    private fun rewritePrivateLanBackendOriginToBase(absoluteUrl: String, apiBase: String, includeCoverPaths: Boolean): String {
        val urlMatch = ABS_HTTP_URL.find(absoluteUrl) ?: return absoluteUrl
        val urlScheme = absoluteUrl.substringBefore("://", missingDelimiterValue = "").lowercase()
        val urlAuthority = urlMatch.groupValues[1]
        val rest = urlMatch.groupValues[2].ifEmpty { "/" }
        if (!isPrivateOrLoopbackIpv4Host(hostOfAuthority(urlAuthority))) return absoluteUrl
        val rewritable = isBackendAudioOrApiPath(rest) || (includeCoverPaths && isBackendCoverPath(rest))
        if (!rewritable) return absoluteUrl

        val baseMatch = ABS_HTTP_URL.find(apiBase) ?: return absoluteUrl
        val baseScheme = apiBase.substringBefore("://", missingDelimiterValue = "").lowercase()
        val baseAuthority = baseMatch.groupValues[1]
        if (urlScheme != baseScheme) return absoluteUrl
        if (effectivePort(urlScheme, urlAuthority) != effectivePort(baseScheme, baseAuthority)) return absoluteUrl

        return "$baseScheme://$baseAuthority$rest"
    }

    private fun normalizeAbsoluteHttpUrl(absoluteUrl: String, apiBase: String, includeCoverPaths: Boolean): String {
        val base = apiBase.trimEnd('/')
        var u = rewriteLocalhostAliasesToBase(absoluteUrl, base)
        u = rewritePrivateLanBackendOriginToBase(u, base, includeCoverPaths)
        return u
    }

    /** Resolves audioFileUrl to a playable absolute URL. Backend may return full URLs or relative paths.
     * Paths starting with "stories/" must be served via /audio/stories/... proxy.
     * Rewrites localhost/127.0.0.1/10.0.2.2 to apiBaseUrl so each platform reaches the host
     * (10.0.2.2 = host only on Android emulator; iOS Simulator uses localhost).
     * Also rewrites LAN/private IPs for `/audio/` and `/api/` paths when the port matches [apiBaseUrl]. */
    fun resolveAudioUrl(apiBaseUrl: String, audioFileUrl: String?): String? {
        if (audioFileUrl.isNullOrBlank()) return null
        val base = apiBaseUrl.trimEnd('/')
        return when {
            audioFileUrl.startsWith("http://") || audioFileUrl.startsWith("https://") ->
                normalizeAbsoluteHttpUrl(audioFileUrl, base, includeCoverPaths = false)
            audioFileUrl.startsWith("/audio/") -> base + audioFileUrl
            audioFileUrl.startsWith("/") -> base + audioFileUrl
            audioFileUrl.startsWith("stories/") -> "$base/audio/$audioFileUrl"
            else -> "$base/$audioFileUrl"
        }
    }

    /** Resolves coverImageUrl to a loadable absolute URL. Backend may return proxy paths or full URLs.
     * Rewrites localhost/127.0.0.1/10.0.2.2 to apiBaseUrl so each platform reaches the host.
     * Also rewrites LAN/private IPs for `/audio/`, `/api/`, and `/covers/` when the port matches [apiBaseUrl]. */
    fun resolveCoverUrl(apiBaseUrl: String, coverImageUrl: String?): String? {
        if (coverImageUrl.isNullOrBlank()) return null
        val base = apiBaseUrl.trimEnd('/')
        return when {
            coverImageUrl.startsWith("http://") || coverImageUrl.startsWith("https://") ->
                normalizeAbsoluteHttpUrl(coverImageUrl, base, includeCoverPaths = true)
            coverImageUrl.startsWith("/") -> base + coverImageUrl
            else -> "$base/$coverImageUrl"
        }
    }
    /** Web app URL for subscription management (subscribe/manage plan). */
    const val SUBSCRIPTION_WEB_URL = "https://app.tamixa.com/subscription"
    const val CONNECT_TIMEOUT_MS = 30_000L
    const val SOCKET_TIMEOUT_MS = 30_000L
    const val RETRY_MAX_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L
    const val REFRESH_BUFFER_SECONDS = 60L // refresh if expiry within 60s
}
