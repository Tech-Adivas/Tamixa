package com.tamixa.infrastructure.narration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

/** Steady narration: paragraph pauses kept, storyteller instructions for gpt-4o TTS, no silently skipped parts. */
class OpenAITtsClientAdapterTest {

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val url = "https://oa.test/v1/audio/speech"

    private fun adapter(model: String, speed: Double = 1.0, instructions: String = "Calm, steady storyteller.") =
        OpenAITtsClientAdapter(restTemplate, ObjectMapper(), null, "sk-test", "https://oa.test", model, speed, instructions)

    private val ssml = """
        <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis">
            <lang xml:lang="en-IN">
                <prosody rate="100%">
                    Once upon a time,<break time="120ms"/> a bird sang.<break time="250ms"/>
        The moon rose &amp; smiled.
                </prosody>
            </lang>
        </speak>
    """.trimIndent()

    @Test
    fun `gpt-4o-mini-tts gets the storyteller instructions and the narration keeps its paragraph pause`() {
        server.expect(requestTo(url))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.model").value("gpt-4o-mini-tts"))
            .andExpect(jsonPath("$.input").value("Once upon a time, a bird sang.\n\nThe moon rose & smiled."))
            .andExpect(jsonPath("$.instructions").value("Calm, steady storyteller."))
            .andExpect(jsonPath("$.speed").doesNotExist())
            .andRespond(withSuccess(byteArrayOf(7), MediaType.parseMediaType("audio/mpeg")))

        assertThat(adapter("gpt-4o-mini-tts").synthesizeToMp3(ssml, "en", "default")).containsExactly(7)
        server.verify()
    }

    @Test
    fun `tts-1-hd gets no instructions and keeps a configured speed`() {
        server.expect(requestTo(url))
            .andExpect(jsonPath("$.model").value("tts-1-hd"))
            .andExpect(jsonPath("$.instructions").doesNotExist())
            .andExpect(jsonPath("$.speed").value(0.9))
            .andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))

        adapter("tts-1-hd", speed = 0.9).synthesizeToMp3(ssml, "en", "default")
        server.verify()
    }

    @Test
    fun `an empty part fails the narration instead of skipping a piece of the story`() {
        val paragraph = "The little elephant walked slowly to the river and looked at the stars. "
        val longSsml = "<speak>" + (1..20).joinToString("<break time=\"250ms\"/>\n") { paragraph } + "</speak>" // > 1000 chars
        server.expect(requestTo(url)).andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))
        server.expect(ExpectedCount.once(), requestTo(url)).andRespond(withSuccess(ByteArray(0), MediaType.parseMediaType("audio/mpeg")))

        assertThat(adapter("gpt-4o-mini-tts").synthesizeToMp3(longSsml, "en", "default")).isNull()
        server.verify()
    }
}
