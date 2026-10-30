package com.tamixa.ui.edu

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InteractiveStoryGraphTest {

    private fun parse(json: String) = parseInteractiveStoryGraph(Json.parseToJsonElement(json).jsonObject)

    @Test
    fun parsesGraphWithoutPictures_likeStoriesMadeBeforeThem() {
        val graph = assertNotNull(
            parse("""{"startSegmentId":"hook","segments":{"hook":{"audioUrl":"a","text":"Hi. Go?","choices":[]}}}""")
        )
        val hook = graph.segments.getValue("hook")
        assertNull(hook.imageUrl)
        assertNull(hook.question)
    }

    @Test
    fun parsesImageUrlAndQuestion() {
        val graph = assertNotNull(
            parse(
                """{"startSegmentId":"hook","segments":{"hook":{"audioUrl":"a","text":"x",
                   "imageUrl":"/api/v1/covers/interactive_images/7/hook.png?v=1","question":"Which way?","choices":[]}}}"""
            )
        )
        val hook = graph.segments.getValue("hook")
        assertEquals("/api/v1/covers/interactive_images/7/hook.png?v=1", hook.imageUrl)
        assertEquals("Which way?", hook.questionText())
    }

    @Test
    fun questionText_isTheNarrationsClosingQuestion() {
        val tamil = InteractiveSegment(
            audioUrl = "a",
            text = "மீனா வானத்தைப் பார்க்கிறாள். இப்போது அவள் என்ன செய்ய வேண்டும்?",
        )
        assertEquals("இப்போது அவள் என்ன செய்ய வேண்டும்?", tamil.questionText())
        val english = InteractiveSegment(audioUrl = "a", text = "Why is the sky blue? Nobody knew. Mia stops. Left or right?")
        assertEquals("Left or right?", english.questionText())
    }

    @Test
    fun questionText_ignoresAnEarlierRhetoricalQuestion() {
        val seg = InteractiveSegment(audioUrl = "a", text = "Why is the sky blue? Nobody knew. Mia walked home.")
        assertNull(seg.questionText())
    }
}
