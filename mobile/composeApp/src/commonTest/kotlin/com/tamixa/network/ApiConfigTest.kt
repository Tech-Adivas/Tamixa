package com.tamixa.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [ApiConfig] URL resolution (audit recommendation).
 */
class ApiConfigTest {

    private val base = "https://api.example.com"

    @Test
    fun resolveAudioUrl_nullOrBlank_returnsNull() {
        assertNull(ApiConfig.resolveAudioUrl(base, null))
        assertNull(ApiConfig.resolveAudioUrl(base, ""))
        assertNull(ApiConfig.resolveAudioUrl(base, "   "))
    }

    @Test
    fun resolveAudioUrl_absoluteUrl_unchangedWhenNoLocalhost() {
        val url = "https://cdn.example.com/audio/story.mp3"
        assertEquals(url, ApiConfig.resolveAudioUrl(base, url))
    }

    @Test
    fun resolveAudioUrl_localhost_rewrittenToBase() {
        assertEquals(
            "https://api.example.com/path",
            ApiConfig.resolveAudioUrl(base, "http://localhost:8080/path")
        )
        assertEquals(
            "https://api.example.com/audio/x",
            ApiConfig.resolveAudioUrl(base, "https://127.0.0.1:8080/audio/x")
        )
        assertEquals(
            "https://api.example.com/stream",
            ApiConfig.resolveAudioUrl(base, "http://10.0.2.2:8080/stream")
        )
    }

    @Test
    fun resolveAudioUrl_storiesPrefix_prefixedWithAudio() {
        assertEquals(
            "https://api.example.com/audio/stories/123.mp3",
            ApiConfig.resolveAudioUrl(base, "stories/123.mp3")
        )
    }

    @Test
    fun resolveAudioUrl_leadingSlash_prefixedWithBase() {
        assertEquals(
            "https://api.example.com/audio/file.mp3",
            ApiConfig.resolveAudioUrl(base, "/audio/file.mp3")
        )
    }

    @Test
    fun resolveAudioUrl_baseUrlTrailingSlash_trimmed() {
        assertEquals(
            "https://api.example.com/path",
            ApiConfig.resolveAudioUrl("$base/", "/path")
        )
    }

    @Test
    fun resolveCoverUrl_nullOrBlank_returnsNull() {
        assertNull(ApiConfig.resolveCoverUrl(base, null))
        assertNull(ApiConfig.resolveCoverUrl(base, ""))
    }

    @Test
    fun resolveCoverUrl_absoluteUrl_unchangedWhenNoLocalhost() {
        val url = "https://cdn.example.com/cover.png"
        assertEquals(url, ApiConfig.resolveCoverUrl(base, url))
    }

    @Test
    fun resolveCoverUrl_localhost_rewrittenToBase() {
        assertEquals(
            "https://api.example.com/covers/1.png",
            ApiConfig.resolveCoverUrl(base, "http://localhost:8080/covers/1.png")
        )
    }

    @Test
    fun resolveCoverUrl_leadingSlash_prefixedWithBase() {
        assertEquals(
            "https://api.example.com/covers/x.png",
            ApiConfig.resolveCoverUrl(base, "/covers/x.png")
        )
    }
}
