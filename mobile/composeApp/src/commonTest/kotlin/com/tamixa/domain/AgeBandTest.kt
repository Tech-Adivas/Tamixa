package com.tamixa.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgeBandTest {

    private fun story(id: Long, age: Int) = Story(
        id = id,
        parentId = 0L,
        childId = null,
        content = "Story",
        theme = "Friendship",
        language = "ta",
        age = age,
        childName = "Listener",
        wordCount = 100,
        readingTimeMinutes = 1.0,
        createdAt = "2026-10-01T00:00:00Z",
    )

    @Test
    fun bandsCoverFiveToTenWithoutGaps() {
        (5..10).forEach { age -> assertEquals(1, AgeBand.entries.count { it.includes(age) }, "age $age") }
        assertEquals(0, AgeBand.entries.count { it.includes(4) })
        assertEquals(0, AgeBand.entries.count { it.includes(11) })
    }

    @Test
    fun fromKey_readsSavedKeysAndTreatsUnknownAsAllAges() {
        assertEquals(AgeBand.AGES_7_8, AgeBand.fromKey("7-8"))
        assertEquals(AgeBand.AGES_9_10, AgeBand.fromKey(" 9-10 "))
        assertNull(AgeBand.fromKey(""))
        assertNull(AgeBand.fromKey(null))
        assertNull(AgeBand.fromKey("3-4"))
    }

    @Test
    fun filterByAgeBand_keepsStoriesInsideBand() {
        val stories = listOf(story(1, 5), story(2, 6), story(3, 7), story(4, 10), story(5, 35))
        assertEquals(listOf(1L, 2L), stories.filterByAgeBand(AgeBand.AGES_5_6).map { it.id })
        assertEquals(listOf(3L), stories.filterByAgeBand(AgeBand.AGES_7_8).map { it.id })
        assertEquals(listOf(4L), stories.filterByAgeBand(AgeBand.AGES_9_10).map { it.id })
    }

    @Test
    fun filterByAgeBand_nullBandKeepsEverything() {
        val stories = listOf(story(1, 5), story(2, 35))
        assertEquals(stories, stories.filterByAgeBand(null))
    }
}
