package com.tamixa.application.narration

import com.tamixa.application.narration.impl.EmotionTaggingServiceImpl
import com.tamixa.domain.narration.EmotionTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for deterministic emotion tagging.
 * Verifies no hallucination: original text preserved, only labels assigned.
 */
class EmotionTaggingServiceImplTest {

    private val service = EmotionTaggingServiceImpl()

    @Test
    fun `tagEmotions returns CALM for plain text`() {
        val script = "Once upon a time there was a little rabbit."
        val result = service.tagEmotions(script)
        assertEquals(script, result.originalScript)
        assertTrue(result.segments.all { it.emotion == EmotionTag.CALM })
        assertTrue(result.validateWordCountTolerance(15))
    }

    @Test
    fun `tagEmotions detects DIALOGUE for quoted text`() {
        val script = "The fox said \"Hello little rabbit\" and smiled."
        val result = service.tagEmotions(script)
        val dialogueSegments = result.segments.filter { it.emotion == EmotionTag.DIALOGUE }
        assertTrue(dialogueSegments.any { it.text.contains("Hello") })
        assertTrue(result.validateWordCountTolerance(15))
    }

    @Test
    fun `tagEmotions detects DIALOGUE for curly quoted text`() {
        val script = "The fox said “Hello little rabbit” and smiled."
        val result = service.tagEmotions(script)
        val dialogueSegments = result.segments.filter { it.emotion == EmotionTag.DIALOGUE }
        assertTrue(dialogueSegments.any { it.text.contains("Hello little rabbit") })
    }

    @Test
    fun `tagEmotions detects EXCITED for exclamation-heavy text`() {
        val script = "Wow! Amazing! What a day!"
        val result = service.tagEmotions(script)
        assertTrue(result.segments.any { it.emotion == EmotionTag.EXCITED })
        assertTrue(result.validateWordCountTolerance(15))
    }

    @Test
    fun `tagEmotions detects WHISPER for softly keyword`() {
        val script = "She spoke softly to the child."
        val result = service.tagEmotions(script)
        assertTrue(result.segments.any { it.emotion == EmotionTag.WHISPER })
    }

    @Test
    fun `tagEmotions detects SOFT_SUSPENSE for suddenly`() {
        val script = "Suddenly, the door opened."
        val result = service.tagEmotions(script)
        assertTrue(result.segments.any { it.emotion == EmotionTag.SOFT_SUSPENSE })
    }

    @Test
    fun `tagEmotions detects CONVERSATIONAL for questions and engaging phrases`() {
        val scriptQuestion = "What do you think happened next? The little rabbit smiled."
        val resultQuestion = service.tagEmotions(scriptQuestion)
        assertTrue(resultQuestion.segments.any { it.emotion == EmotionTag.CONVERSATIONAL })

        val scriptImagine = "Can you imagine? A tiny dragon appeared."
        val resultImagine = service.tagEmotions(scriptImagine)
        assertTrue(resultImagine.segments.any { it.emotion == EmotionTag.CONVERSATIONAL })
    }

    @Test
    fun `tagEmotions detects CONVERSATIONAL for Tamil native interjections`() {
        val script = "அடடா, மீரா ஒரு அழகான பட்டாம் பூச்சியை பார்த்தாள். பாருங்க!"
        val result = service.tagEmotions(script, "ta")
        assertTrue(result.segments.any { it.emotion == EmotionTag.CONVERSATIONAL })
    }

    @Test
    fun `tagEmotions detects CONVERSATIONAL for Hindi native interjections`() {
        val script = "अरे, मीरा ने एक सुंदर तितली देखी। देखो कितनी प्यारी!"
        val result = service.tagEmotions(script, "hi")
        assertTrue(result.segments.any { it.emotion == EmotionTag.CONVERSATIONAL })
    }

    @Test
    fun `tagEmotions detects EXCITED for non english exclamations`() {
        val script = "வாவ்! இன்று என்ன ஒரு அழகான நாள்!"
        val result = service.tagEmotions(script, "ta")
        assertTrue(result.segments.any { it.emotion == EmotionTag.EXCITED })
    }

    @Test
    fun `validateWordCountTolerance rejects large deviation`() {
        val result = com.tamixa.domain.narration.EmotionTaggedScript(
            originalScript = "a b c",
            segments = listOf(
                com.tamixa.domain.narration.EmotionSegment("a", EmotionTag.CALM),
                com.tamixa.domain.narration.EmotionSegment("b", EmotionTag.CALM)
            ),
            wordCount = 3
        )
        // actual=2, expected=3 -> 33% deviation
        assertTrue(!result.validateWordCountTolerance(15))
    }
}
