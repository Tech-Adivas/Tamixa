package com.tamixa.infrastructure.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HostStoryClipUrlTest {

    @Test
    fun `blank host clip yields null`() {
        val p = AppProperties(audio = AppProperties.AudioProperties(hostStoryClipUrl = "  "))
        assertNull(p.resolvedHostStoryClipUrl())
    }

    @Test
    fun `https url passed through`() {
        val p = AppProperties(
            audio = AppProperties.AudioProperties(hostStoryClipUrl = "https://cdn.example.com/x.mp4")
        )
        assertEquals("https://cdn.example.com/x.mp4", p.resolvedHostStoryClipUrl())
    }

    @Test
    fun `relative path joins public base`() {
        val p = AppProperties(
            audio = AppProperties.AudioProperties(
                publicBaseUrl = "https://api.example.com",
                hostStoryClipUrl = "clips/host.mp4"
            )
        )
        assertEquals("https://api.example.com/clips/host.mp4", p.resolvedHostStoryClipUrl())
    }

    @Test
    fun `leading slash path joins base without double slash`() {
        val p = AppProperties(
            audio = AppProperties.AudioProperties(
                publicBaseUrl = "https://api.example.com/",
                hostStoryClipUrl = "/static/bumper.mp4"
            )
        )
        assertEquals("https://api.example.com/static/bumper.mp4", p.resolvedHostStoryClipUrl())
    }
}
