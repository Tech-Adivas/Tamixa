package com.tamixa.infrastructure.narration

import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.ElevenLabsVoiceSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** ElevenLabs narrator: right voice per profile, paragraph text (pauses kept), nothing sent without a voice. */
class ElevenLabsTtsClientAdapterTest {

    private val elevenLabs: ElevenLabsVoiceCloningPort = mock()
    private val ssml = """
        <speak><lang xml:lang="ta-IN"><prosody rate="100%">
        [Warm tone] ஒரு காலத்தில்,<break time="120ms"/> மீரா வாழ்ந்தாள்.<break time="250ms"/>
        “வா” என்றது பறவை.
        </prosody></lang></speak>
    """.trimIndent()

    @Test
    fun `default narration uses the default voice and keeps the paragraph pause`() {
        whenever(elevenLabs.synthesize(any(), any(), any(), any())).thenReturn(byteArrayOf(5))
        val adapter = ElevenLabsTtsClientAdapter(elevenLabs, "nila-voice", "", stability = 0.7, similarityBoost = 0.9, speed = 0.9)

        assertThat(adapter.synthesizeToMp3(ssml, "ta", "default")).containsExactly(5)
        // The narrator's own settings, not the cloned-voice ones.
        verify(elevenLabs).synthesize(
            eq("ஒரு காலத்தில், மீரா வாழ்ந்தாள்.\n\n“வா” என்றது பறவை."), eq("nila-voice"), eq("ta"),
            eq(ElevenLabsVoiceSettings(stability = 0.7, similarityBoost = 0.9, speed = 0.9)),
        )
    }

    @Test
    fun `calm uses its own voice, or the default when none is set`() {
        assertThat(ElevenLabsTtsClientAdapter(elevenLabs, "nila", "srivi").voiceIdFor("calm")).isEqualTo("srivi")
        assertThat(ElevenLabsTtsClientAdapter(elevenLabs, "nila", " ").voiceIdFor("calm")).isEqualTo("nila")
        assertThat(ElevenLabsTtsClientAdapter(elevenLabs, "nila", "srivi").voiceIdFor("child-male")).isEqualTo("nila")
    }

    @Test
    fun `no voice configured sends nothing`() {
        assertThat(ElevenLabsTtsClientAdapter(elevenLabs, "", "").synthesizeToMp3(ssml, "ta", "default")).isNull()
        verify(elevenLabs, never()).synthesize(any(), any(), any(), any())
    }
}
