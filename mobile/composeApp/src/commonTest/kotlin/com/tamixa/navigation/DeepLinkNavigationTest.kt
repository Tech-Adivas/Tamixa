package com.tamixa.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkNavigationTest {

    @Test
    fun parseStoryId_tamixaScheme() {
        assertEquals(42L, parseStoryIdFromDeepLink("tamixa://story/42"))
        assertEquals(1L, parseStoryIdFromDeepLink("tamixa://story/1?extra=1"))
    }

    @Test
    fun parseStoryId_shortPath() {
        assertEquals(99L, parseStoryIdFromDeepLink("https://app.example/s/99"))
    }

    @Test
    fun parseStoryId_invalid() {
        assertNull(parseStoryIdFromDeepLink("https://example.com/login"))
    }
}
