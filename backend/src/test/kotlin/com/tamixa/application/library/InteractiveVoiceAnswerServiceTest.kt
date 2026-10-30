package com.tamixa.application.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.SpeechToTextPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class InteractiveVoiceAnswerServiceTest {

    private val mapper = ObjectMapper()
    private val stt: SpeechToTextPort = mock()
    private val llm: OpenAIPort = mock()
    private val service = InteractiveVoiceAnswerService(stt, llm, mapper)
    private val audio = byteArrayOf(1, 2, 3)

    private val segment = mapper.readTree(
        """{"text":"நிலா தயாராக இருக்கிறாள். இப்போது அவள் என்ன செய்ய வேண்டும்?","choices":[
           {"id":"c1","label":"விண்வெளி மையம் செல்ல","nextSegmentId":"space"},
           {"id":"c2","label":"AI உதவியுடன் புதிர்களைத் தீர்க்க","nextSegmentId":"puzzles"}]}"""
    )

    private fun heard(text: String?) =
        whenever(stt.transcribe(any(), any(), any(), anyOrNull())).thenReturn(text)

    @Test
    fun wordMatch_answersWithoutCallingTheLlm_andGivesNoAnswerHintToTranscription() {
        heard("விண்வெளி மையம் செல்ல")
        val r = service.answer(73, "hook", segment, "ta", audio, "a.m4a")
        assertEquals("c1", r.choiceId)
        assertEquals("text", r.method)
        assertEquals("விண்வெளி மையம் செல்ல", r.heardText)
        verify(llm, never()).completeChat(any(), any(), any())
        // A hint with the answers makes the model write them into the transcript.
        verify(stt).transcribe(eq(audio), eq("a.m4a"), eq("ta"), isNull())
    }

    @Test
    fun transcriptContainingEveryAnswer_isNotAMatch() {
        heard("விண்வெளி மையம் செல்ல, AI உதவியுடன் புதிர்களைத் தீர்க்க.")
        whenever(llm.completeChat(any(), any(), any())).thenReturn("""{"choiceId":null}""")
        assertNull(service.answer(73, "hook", segment, "ta", audio, "a.m4a").choiceId)
    }

    @Test
    fun ownWords_fallBackToLlm() {
        heard("ஸ்பேஸ் சென்டர் போகணும்")
        whenever(llm.completeChat(any(), any(), any())).thenReturn("""{"choiceId":"c1"}""")
        val r = service.answer(73, "hook", segment, "ta", audio, "a.m4a")
        assertEquals("c1", r.choiceId)
        assertEquals("ai", r.method)
    }

    @Test
    fun llmUnsureOrInventedId_meansNoMatch() {
        heard("எனக்கு தெரியாது")
        whenever(llm.completeChat(any(), any(), any())).thenReturn("""{"choiceId":null}""", """{"choiceId":"c9"}""", "not json")
        repeat(3) {
            val r = service.answer(73, "hook", segment, "ta", audio, "a.m4a")
            assertNull(r.choiceId)
            assertEquals("none", r.method)
        }
    }

    @Test
    fun miss_isRetriedOnceWithLanguageDetected() {
        whenever(stt.transcribe(any(), any(), eq("ta"), anyOrNull())).thenReturn("நான் அறையில் செல்லவேண்டும்")
        whenever(stt.transcribe(any(), any(), eq(""), anyOrNull())).thenReturn("I want to go to the space centre")
        whenever(llm.completeChat(any(), any(), any())).thenReturn("""{"choiceId":null}""", """{"choiceId":"c1"}""")
        val r = service.answer(73, "hook", segment, "ta", audio, "a.m4a")
        assertEquals("c1", r.choiceId)
        assertEquals("I want to go to the space centre", r.heardText)
    }

    @Test
    fun nothingHeard_noLlmCall() {
        heard(null)
        val r = service.answer(73, "hook", segment, "ta", audio, "a.m4a")
        assertNull(r.choiceId)
        assertEquals("", r.heardText)
        verify(llm, never()).completeChat(any(), any(), any())
    }

    @Test
    fun segmentWithoutChoices_isRejected() {
        val end = mapper.readTree("""{"text":"The end.","choices":[]}""")
        assertThrows<IllegalArgumentException> { service.answer(73, "end", end, "ta", audio, "a.m4a") }
    }
}
