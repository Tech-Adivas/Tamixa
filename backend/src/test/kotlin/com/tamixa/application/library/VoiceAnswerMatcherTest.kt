package com.tamixa.application.library

import com.tamixa.application.library.VoiceAnswerMatcher.Choice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VoiceAnswerMatcherTest {

    private val tamil = listOf(
        Choice("c1", "விண்வெளி மையம் செல்ல"),
        Choice("c2", "AI உதவியுடன் புதிர்களைத் தீர்க்க"),
    )
    private val english = listOf(Choice("left", "Go left to the river"), Choice("right", "Go right to the forest"))

    @Test
    fun exactLabel_matches() {
        assertEquals("c1", VoiceAnswerMatcher.match("விண்வெளி மையம் செல்ல", tamil)?.id)
        assertEquals("c2", VoiceAnswerMatcher.match("AI உதவியுடன் புதிர்களைத் தீர்க்க.", tamil)?.id)
    }

    @Test
    fun tamilWordWithDifferentEnding_matches() {
        // "மையத்திற்கு செல்லலாம்" — same words as the label with other suffixes
        assertEquals("c1", VoiceAnswerMatcher.match("விண்வெளி மையத்திற்கு செல்லலாம்", tamil)?.id)
    }

    @Test
    fun partOfTheAnswerInASentence_matches() {
        assertEquals("right", VoiceAnswerMatcher.match("um I think go right to the forest please", english)?.id)
        assertEquals("c2", VoiceAnswerMatcher.match("புதிர்களைத் தீர்க்க வேண்டும்", tamil)?.id)
    }

    @Test
    fun unclearOrBoth_returnsNull() {
        assertNull(VoiceAnswerMatcher.match("go", english), "both answers start with 'go'")
        assertNull(VoiceAnswerMatcher.match("எனக்கு தெரியாது", tamil))
        assertNull(VoiceAnswerMatcher.match("", tamil))
    }

    @Test
    fun normalize_keepsTamilVowelSignsAndDropsPunctuation() {
        assertEquals("இப்போது அவள் என்ன", VoiceAnswerMatcher.normalize("இப்போது, அவள் என்ன?!"))
    }
}
