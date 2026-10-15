package com.tamixa.infrastructure.voice

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

/** Cloned narration asks ElevenLabs to follow the parent's recording closely (voice_settings). No real API call. */
class ElevenLabsVoiceCloningAdapterVoiceSettingsTest {

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()

    @Test
    fun `speech request carries the likeness settings`() {
        val adapter = ElevenLabsVoiceCloningAdapter(restTemplate, ObjectMapper(), "test-key", "https://el.test")
        server.expect(requestTo("https://el.test/v1/text-to-speech/voice-1?output_format=mp3_44100_128"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.model_id").value("eleven_multilingual_v2"))
            .andExpect(jsonPath("$.voice_settings.similarity_boost").value(0.9))
            .andExpect(jsonPath("$.voice_settings.stability").value(0.5))
            .andExpect(jsonPath("$.voice_settings.style").value(0.0))
            .andExpect(jsonPath("$.voice_settings.use_speaker_boost").value(true))
            .andRespond(withSuccess(byteArrayOf(1, 2, 3), MediaType.parseMediaType("audio/mpeg")))

        val audio = adapter.synthesize("Hello", "voice-1", "ta")

        assertThat(audio).containsExactly(1, 2, 3)
        server.verify()
    }

    @Test
    fun `configured values are used and kept within 0 to 1`() {
        val adapter = ElevenLabsVoiceCloningAdapter(
            restTemplate, ObjectMapper(), "test-key", "https://el.test",
            stability = 0.3, similarityBoost = 1.7, style = 0.2, speakerBoost = false
        )
        server.expect(requestTo("https://el.test/v1/text-to-speech/voice-1?output_format=mp3_44100_128"))
            .andExpect(jsonPath("$.voice_settings.stability").value(0.3))
            .andExpect(jsonPath("$.voice_settings.similarity_boost").value(1.0))
            .andExpect(jsonPath("$.voice_settings.style").value(0.2))
            .andExpect(jsonPath("$.voice_settings.use_speaker_boost").value(false))
            .andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))

        adapter.synthesize("Hello", "voice-1", "en")

        server.verify()
    }
}
