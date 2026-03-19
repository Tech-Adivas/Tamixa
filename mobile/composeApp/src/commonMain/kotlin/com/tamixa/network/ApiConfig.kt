package com.tamixa.network

object ApiConfig {
    const val API_VERSION = "/api/v1"
    /** Android emulator: 10.0.2.2 = host localhost. iOS Simulator: use localhost. Physical device: use machine IP. */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

    /** Resolves audioFileUrl to a playable absolute URL. Backend may return full URLs or relative paths.
     * Paths starting with "stories/" must be served via /audio/stories/... proxy.
     * Rewrites localhost/127.0.0.1/10.0.2.2 to apiBaseUrl so each platform reaches the host
     * (10.0.2.2 = host only on Android emulator; iOS Simulator uses localhost). */
    fun resolveAudioUrl(apiBaseUrl: String, audioFileUrl: String?): String? {
        if (audioFileUrl.isNullOrBlank()) return null
        val base = apiBaseUrl.trimEnd('/')
        return when {
            audioFileUrl.startsWith("http://") || audioFileUrl.startsWith("https://") -> {
                if (audioFileUrl.contains("localhost") || audioFileUrl.contains("127.0.0.1") || audioFileUrl.contains("10.0.2.2")) {
                    audioFileUrl
                        .replace(Regex("https?://localhost(?::\\d+)?"), base)
                        .replace(Regex("https?://127\\.0\\.0\\.1(?::\\d+)?"), base)
                        .replace(Regex("https?://10\\.0\\.2\\.2(?::\\d+)?"), base)
                } else audioFileUrl
            }
            audioFileUrl.startsWith("/audio/") -> base + audioFileUrl
            audioFileUrl.startsWith("/") -> base + audioFileUrl
            audioFileUrl.startsWith("stories/") -> "$base/audio/$audioFileUrl"
            else -> "$base/$audioFileUrl"
        }
    }

    /** Resolves coverImageUrl to a loadable absolute URL. Backend may return proxy paths or full URLs.
     * Rewrites localhost/127.0.0.1/10.0.2.2 to apiBaseUrl so each platform reaches the host. */
    fun resolveCoverUrl(apiBaseUrl: String, coverImageUrl: String?): String? {
        if (coverImageUrl.isNullOrBlank()) return null
        val base = apiBaseUrl.trimEnd('/')
        return when {
            coverImageUrl.startsWith("http://") || coverImageUrl.startsWith("https://") -> {
                if (coverImageUrl.contains("localhost") || coverImageUrl.contains("127.0.0.1") || coverImageUrl.contains("10.0.2.2")) {
                    coverImageUrl
                        .replace(Regex("https?://localhost(?::\\d+)?"), base)
                        .replace(Regex("https?://127\\.0\\.0\\.1(?::\\d+)?"), base)
                        .replace(Regex("https?://10\\.0\\.2\\.2(?::\\d+)?"), base)
                } else coverImageUrl
            }
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
