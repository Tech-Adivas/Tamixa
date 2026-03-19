package com.tamixa.application.narration

import com.tamixa.application.narration.impl.SSMLBuilderServiceImpl
import com.tamixa.domain.narration.EmotionSegment
import com.tamixa.domain.narration.EmotionTag
import com.tamixa.domain.narration.EmotionTaggedScript
import com.tamixa.domain.narration.ToneMode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for SSML emotion-to-prosody mapping.
 */
class SSMLEmotionMappingTest {

    private val ssmlBuilder = SSMLBuilderServiceImpl()  // natural pace (default)
    private val expressiveBuilder = SSMLBuilderServiceImpl(false)  // expressive: emotion prosody

    @Test
    fun `buildSSMLFromEmotionTagged natural pace with subtle prosody adds light pitch for DIALOGUE`() {
        val tagged = EmotionTaggedScript(
            originalScript = "Hello \"world\"",
            segments = listOf(
                EmotionSegment("Hello ", EmotionTag.CALM),
                EmotionSegment("\"world\"", EmotionTag.DIALOGUE)
            ),
            wordCount = 2
        )
        val result = ssmlBuilder.buildSSMLFromEmotionTagged(tagged, "en", 6, ToneMode.CALM)
        assertTrue(result.contains("rate=\"100%\""), "Natural pace: rate=100%")
        assertTrue(result.contains("pitch=\"+2%\""), "Natural pace with subtle prosody: light pitch for DIALOGUE warmth")
    }

    @Test
    fun `buildSSMLFromEmotionTagged expressive adds pitch for DIALOGUE`() {
        val tagged = EmotionTaggedScript(
            originalScript = "Hello \"world\"",
            segments = listOf(
                EmotionSegment("Hello ", EmotionTag.CALM),
                EmotionSegment("\"world\"", EmotionTag.DIALOGUE)
            ),
            wordCount = 2
        )
        val result = expressiveBuilder.buildSSMLFromEmotionTagged(tagged, "en", 6, ToneMode.CALM)
        assertTrue(result.contains("pitch=\"+6%\""), "DIALOGUE gets pronounced pitch lift for quoted speech")
    }

    @Test
    fun `buildSSMLFromEmotionTagged expressive adds volume soft for WHISPER`() {
        val tagged = EmotionTaggedScript(
            originalScript = "She whispered",
            segments = listOf(EmotionSegment("She whispered", EmotionTag.WHISPER)),
            wordCount = 2
        )
        val result = expressiveBuilder.buildSSMLFromEmotionTagged(tagged, "en", 5, ToneMode.CALM)
        assertTrue(result.contains("volume=\"soft\"") && result.contains("rate=\"85%\""), "WHISPER gets soft volume and slower rate")
    }

    @Test
    fun `buildSSMLFromEmotionTagged uses language-specific break for English`() {
        val tagged = EmotionTaggedScript(
            originalScript = "A. B.",
            segments = listOf(
                EmotionSegment("A.", EmotionTag.CALM),
                EmotionSegment("B.", EmotionTag.CALM)
            ),
            wordCount = 2
        )
        val result = ssmlBuilder.buildSSMLFromEmotionTagged(tagged, "en", 4, ToneMode.CALM)
        assertTrue(result.contains("""time="300ms""""), "English: 300ms per spec (smooth flow)")
    }

    @Test
    fun `buildSSMLFromEmotionTagged uses language-specific break for Tamil`() {
        val tagged = EmotionTaggedScript(
            originalScript = "A. B.",
            segments = listOf(
                EmotionSegment("A.", EmotionTag.CALM),
                EmotionSegment("B.", EmotionTag.CALM)
            ),
            wordCount = 2
        )
        val result = ssmlBuilder.buildSSMLFromEmotionTagged(tagged, "ta", 4, ToneMode.CALM)
        assertTrue(result.contains("""time="300ms""""), "Tamil: 300ms per spec (smooth flow)")
    }

    @Test
    fun `buildSSMLFromEmotionTagged uses natural rate for Tamil`() {
        val tagged = EmotionTaggedScript(
            originalScript = "வணக்கம்",
            segments = listOf(EmotionSegment("வணக்கம்", EmotionTag.CALM)),
            wordCount = 1
        )
        val result = ssmlBuilder.buildSSMLFromEmotionTagged(tagged, "ta", 6, ToneMode.CALM)
        assertTrue(result.contains("rate=\"100%\"") && result.contains("ta-IN"))
    }

    @Test
    fun `buildSSMLFromEmotionTagged expressive adds pitch for CONVERSATIONAL`() {
        val tagged = EmotionTaggedScript(
            originalScript = "What do you think? Hello.",
            segments = listOf(
                EmotionSegment("What do you think? ", EmotionTag.CONVERSATIONAL),
                EmotionSegment("Hello.", EmotionTag.CALM)
            ),
            wordCount = 5
        )
        val result = expressiveBuilder.buildSSMLFromEmotionTagged(tagged, "en", 5, ToneMode.CALM)
        assertTrue(result.contains("pitch=\"+4%\""), "CONVERSATIONAL gets warm pitch lift")
    }
}
