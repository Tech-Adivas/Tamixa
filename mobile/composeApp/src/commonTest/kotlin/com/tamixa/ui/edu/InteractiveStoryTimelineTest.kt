package com.tamixa.ui.edu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteractiveStoryTimelineTest {

    private fun words(n: Int) = List(n) { "w" }.joinToString(" ")

    /** hook (40 words) asks a question; "long" (30) asks another before "end" (30); "short" (10) ends right away. */
    private val graph = InteractiveStoryGraph(
        startSegmentId = "hook",
        segments = mapOf(
            "hook" to InteractiveSegment(
                audioUrl = "a", text = words(40),
                choices = listOf(InteractiveChoice("c1", "Long way", "long"), InteractiveChoice("c2", "Short way", "short")),
            ),
            "long" to InteractiveSegment(audioUrl = "b", text = words(30), choices = listOf(InteractiveChoice("c3", "Go on", "end"))),
            "end" to InteractiveSegment(audioUrl = "c", text = words(30)),
            "short" to InteractiveSegment(audioUrl = "d", text = words(10)),
        ),
    )
    private val timeline = InteractiveStoryTimeline(graph)

    @Test
    fun theFirstSceneIsOnlyPartOfTheWholeStory() {
        // Longest story: 40 + 30 + 30 = 100 words; the end of the hook is 40% through, not 100%.
        assertEquals(0.4f, timeline.progress(emptyList(), "hook", 1f), 0.001f)
        assertEquals(0.2f, timeline.progress(emptyList(), "hook", 0.5f), 0.001f)
    }

    @Test
    fun progressContinuesAcrossScenesAndReachesTheEndOnlyOnTheLastScene() {
        assertEquals(0.4f, timeline.progress(listOf("hook"), "long", 0f), 0.001f)
        assertEquals(0.7f, timeline.progress(listOf("hook"), "long", 1f), 0.001f)
        assertEquals(1f, timeline.progress(listOf("hook", "long"), "end", 1f), 0.001f)
    }

    @Test
    fun takingAShorterBranchNeverMovesProgressBackwards() {
        val beforeChoice = timeline.progress(emptyList(), "hook", 1f)
        val afterChoice = timeline.progress(listOf("hook"), "short", 0f)
        assertTrue(afterChoice >= beforeChoice, "after=$afterChoice before=$beforeChoice")
        assertEquals(1f, timeline.progress(listOf("hook"), "short", 1f), 0.001f)
    }

    @Test
    fun wholeStoryLengthIsScaledFromWhatWasHeard() {
        // A 20 s hook is 40 of the longest 100 words -> about 50 s for the whole story.
        assertEquals(50, timeline.durationSeconds(emptyList(), "hook", playedSeconds = 0, currentClipSeconds = 20))
        assertEquals(null, timeline.durationSeconds(emptyList(), "hook", playedSeconds = 0, currentClipSeconds = 0))
    }

    @Test
    fun questionMarksSitWhereScenesEndWithAQuestion() {
        assertEquals(listOf(0.4f), timeline.questionMarks(emptyList(), "hook"))
        val marks = timeline.questionMarks(listOf("hook"), "long")
        assertEquals(2, marks.size)
        assertEquals(0.4f, marks[0], 0.001f)
        assertEquals(0.7f, marks[1], 0.001f)
        // The ending asks nothing: only the two questions already answered are marked.
        assertEquals(2, timeline.questionMarks(listOf("hook", "long"), "end").size)
    }

    @Test
    fun scenesWithoutTextCountTheSameAndLoopsDoNotHang() {
        val noText = InteractiveStoryGraph(
            startSegmentId = "a",
            segments = mapOf(
                "a" to InteractiveSegment(audioUrl = "1", choices = listOf(InteractiveChoice("x", "x", "b"))),
                "b" to InteractiveSegment(audioUrl = "2", choices = listOf(InteractiveChoice("y", "back", "a"), InteractiveChoice("z", "on", "c"))),
                "c" to InteractiveSegment(audioUrl = "3"),
            ),
        )
        val t = InteractiveStoryTimeline(noText)
        assertEquals(1f / 3f, t.progress(emptyList(), "a", 1f), 0.001f)
        assertEquals(1f, t.progress(listOf("a", "b"), "c", 1f), 0.001f)
    }
}
