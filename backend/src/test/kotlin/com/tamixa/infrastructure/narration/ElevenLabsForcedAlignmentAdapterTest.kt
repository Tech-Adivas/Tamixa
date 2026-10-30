package com.tamixa.infrastructure.narration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tamixa.application.port.narration.AlignedWord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/** Parsing of the ElevenLabs forced-alignment response (shape taken from a real Tamil story response). */
class ElevenLabsForcedAlignmentAdapterTest {

    private val adapter = ElevenLabsForcedAlignmentAdapter(mock(), jacksonObjectMapper(), "key", "https://api.elevenlabs.io")

    @Test
    fun `keeps real words in order and drops the space entries`() {
        val json = """
            {"characters":[],"loss":0.83,"words":[
              {"text":"மழை","start":0.14,"end":0.44,"loss":0.9},
              {"text":" ","start":0.44,"end":0.5,"loss":0.1},
              {"text":"நின்று","start":0.5,"end":0.9,"loss":0.7},
              {"text":" ","start":0.9,"end":0.95,"loss":0.1},
              {"text":"ஒலிக்கிறது.","start":0.95,"end":1.6,"loss":0.6}
            ]}
        """.trimIndent()

        assertThat(adapter.parseWords(json)).containsExactly(
            AlignedWord("மழை", 0.14, 0.44),
            AlignedWord("நின்று", 0.5, 0.9),
            AlignedWord("ஒலிக்கிறது.", 0.95, 1.6)
        )
    }

    @Test
    fun `missing words list yields no timings`() {
        assertThat(adapter.parseWords("""{"loss":1.0}""")).isEmpty()
    }

    @Test
    fun `blank audio or transcript is not sent`() {
        assertThat(adapter.alignWords(ByteArray(0), "text")).isNull()
        assertThat(adapter.alignWords(byteArrayOf(1), " ")).isNull()
    }
}
