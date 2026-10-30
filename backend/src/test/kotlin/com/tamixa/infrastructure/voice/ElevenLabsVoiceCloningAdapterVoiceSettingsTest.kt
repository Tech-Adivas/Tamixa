package com.tamixa.infrastructure.voice

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.narration.NarrationTextUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ExpectedCount
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

/**
 * Cloned narration asks ElevenLabs to follow the parent's recording closely (voice_settings), sends long stories in
 * parts with the text around each, and cleans the recording when the clone is made. No real API call.
 */
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
            .andExpect(jsonPath("$.voice_settings.speed").value(1.0))
            .andRespond(withSuccess(byteArrayOf(1, 2, 3), MediaType.parseMediaType("audio/mpeg")))

        val audio = adapter.synthesize("Hello", "voice-1", "ta")

        assertThat(audio).containsExactly(1, 2, 3)
        server.verify()
    }

    @Test
    fun `configured values are used and kept within 0 to 1`() {
        val adapter = ElevenLabsVoiceCloningAdapter(
            restTemplate, ObjectMapper(), "test-key", "https://el.test",
            stability = 0.3, similarityBoost = 1.7, style = 0.2, speakerBoost = false, speed = 0.5
        )
        server.expect(requestTo("https://el.test/v1/text-to-speech/voice-1?output_format=mp3_44100_128"))
            .andExpect(jsonPath("$.voice_settings.stability").value(0.3))
            .andExpect(jsonPath("$.voice_settings.similarity_boost").value(1.0))
            .andExpect(jsonPath("$.voice_settings.style").value(0.2))
            .andExpect(jsonPath("$.voice_settings.use_speaker_boost").value(false))
            .andExpect(jsonPath("$.voice_settings.speed").value(0.7))
            .andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))

        adapter.synthesize("Hello", "voice-1", "en")

        server.verify()
    }

    @Test
    fun `long story is sent in parts with the text around each and the audio is joined in order`() {
        val sentence = "The little elephant walked slowly to the river and looked at the stars. "
        val story = sentence.repeat(30) // ~2,100 characters -> 3 parts
        val parts = NarrationTextUtils.splitForSpeech(story, ElevenLabsVoiceCloningAdapter.MAX_CHUNK_CHARS)
        assertThat(parts).hasSize(3)
        val adapter = ElevenLabsVoiceCloningAdapter(restTemplate, ObjectMapper(), "test-key", "https://el.test")
        val url = "https://el.test/v1/text-to-speech/voice-1?output_format=mp3_44100_128"
        server.expect(requestTo(url))
            .andExpect(jsonPath("$.text").value(parts[0]))
            .andExpect(jsonPath("$.previous_text").doesNotExist())
            .andExpect(jsonPath("$.next_text").exists())
            .andExpect(jsonPath("$.language_code").value("ta"))
            .andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))
        server.expect(requestTo(url))
            .andExpect(jsonPath("$.text").value(parts[1]))
            .andExpect(jsonPath("$.previous_text").exists())
            .andExpect(jsonPath("$.next_text").exists())
            .andRespond(withSuccess(byteArrayOf(2), MediaType.parseMediaType("audio/mpeg")))
        server.expect(requestTo(url))
            .andExpect(jsonPath("$.text").value(parts[2]))
            .andExpect(jsonPath("$.next_text").doesNotExist())
            .andRespond(withSuccess(byteArrayOf(3), MediaType.parseMediaType("audio/mpeg")))

        val audio = adapter.synthesize(story, "voice-1", "ta")

        assertThat(audio).containsExactly(1, 2, 3)
        server.verify()
    }

    @Test
    fun `a failed part fails the whole story instead of saving partial audio`() {
        val story = "One sentence here. ".repeat(80) // ~1,500 characters -> 2 parts
        val adapter = ElevenLabsVoiceCloningAdapter(restTemplate, ObjectMapper(), "test-key", "https://el.test")
        val url = "https://el.test/v1/text-to-speech/voice-1?output_format=mp3_44100_128"
        server.expect(requestTo(url)).andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))
        // Second part: empty with language_code, then empty again without it.
        server.expect(ExpectedCount.times(2), requestTo(url))
            .andRespond(withSuccess(ByteArray(0), MediaType.parseMediaType("audio/mpeg")))

        assertThat(adapter.synthesize(story, "voice-1", "en")).isNull()
        server.verify()
    }

    @Test
    fun `configured model is used`() {
        val adapter = ElevenLabsVoiceCloningAdapter(restTemplate, ObjectMapper(), "test-key", "https://el.test", modelRaw = "eleven_v3")
        server.expect(requestTo("https://el.test/v1/text-to-speech/voice-1?output_format=mp3_44100_128"))
            .andExpect(jsonPath("$.model_id").value("eleven_v3"))
            .andRespond(withSuccess(byteArrayOf(1), MediaType.parseMediaType("audio/mpeg")))

        adapter.synthesize("Hello", "voice-1", "en")

        server.verify()
    }

    @Test
    fun `clone is created with background noise removal`() {
        val adapter = ElevenLabsVoiceCloningAdapter(restTemplate, ObjectMapper(), "test-key", "https://el.test")
        server.expect(requestTo("https://el.test/v1/voices/add"))
            .andExpect(method(HttpMethod.POST))
            .andExpect { request ->
                val body = (request as MockClientHttpRequest).bodyAsString
                val field = body.substringAfter("name=\"remove_background_noise\"", "").substringBefore("\r\n--")
                assertThat(field.trim()).endsWith("true")
            }
            .andRespond(withSuccess("""{"voice_id":"v-1"}""", MediaType.APPLICATION_JSON))

        assertThat(adapter.addVoice(byteArrayOf(1, 2, 3), "voice.m4a", "voice")).isEqualTo("v-1")
        server.verify()
    }
}
