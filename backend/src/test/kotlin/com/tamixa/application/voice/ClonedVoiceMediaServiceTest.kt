package com.tamixa.application.voice

import com.tamixa.application.narration.AudioStorageService
import com.tamixa.application.port.StoryVoicePreferenceRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.VoiceProfile
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationAudio
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/** Story narrations in a cloned voice are removed with the voice, and orphans are swept up. */
class ClonedVoiceMediaServiceTest {

    private val narrations: StoryNarrationAudioRepositoryPort = mock()
    private val preferences: StoryVoicePreferenceRepositoryPort = mock()
    private val voices: VoiceRepositoryPort = mock()
    private val storage: AudioStorageService = mock()
    private val service = ClonedVoiceMediaService(narrations, preferences, voices, storage)

    @Test
    fun `detaching a voice removes its narration rows and voice settings and returns their files`() {
        whenever(narrations.findAllByVoiceProfileIn(listOf("cloned:12")))
            .thenReturn(listOf(row(1, "cloned:12", "stories/71/ta/cloned_12.mp3"), row(2, "cloned:12", "stories/71/en/cloned_12.mp3")))

        val files = service.detachMedia(listOf(12L))

        assertThat(files).containsExactly("stories/71/ta/cloned_12.mp3", "stories/71/en/cloned_12.mp3")
        verify(narrations).deleteAllById(listOf(1L, 2L))
        verify(preferences).deleteByVoiceProfileIn(listOf("cloned:12"))
    }

    @Test
    fun `sweep also removes interactive segments recorded in a deleted voice`() {
        whenever(voices.findById(12L)).thenReturn(null)
        whenever(voices.findById(20L)).thenReturn(mock<VoiceProfile>())
        whenever(narrations.findAllByVoiceProfileStartingWith("cloned:")).thenReturn(emptyList())
        whenever(preferences.findDistinctVoiceProfilesStartingWith("cloned:")).thenReturn(emptyList())
        whenever(narrations.findAllByVoiceProfileIn(any())).thenReturn(emptyList())
        whenever(storage.listNarrationKeys(any())).thenReturn(
            sequenceOf(
                "stories/73/ta/cloned_12_seg_hook_1a2b3c4d.mp3",
                "stories/73/ta/cloned_20_seg_hook_1a2b3c4d.mp3",
                "stories/73/ta/interactive_hook_default.mp3",
            )
        )
        whenever(storage.deleteNarrationAudio(any())).thenReturn(true)

        service.reconcileOrphans()

        verify(storage).deleteNarrationAudio("stories/73/ta/cloned_12_seg_hook_1a2b3c4d.mp3")
        verify(storage, never()).deleteNarrationAudio("stories/73/ta/cloned_20_seg_hook_1a2b3c4d.mp3")
        verify(storage, never()).deleteNarrationAudio("stories/73/ta/interactive_hook_default.mp3")
    }

    @Test
    fun `sweep also removes read-along timings of a deleted voice`() {
        whenever(voices.findById(12L)).thenReturn(null)
        whenever(voices.findById(20L)).thenReturn(mock<VoiceProfile>())
        whenever(narrations.findAllByVoiceProfileStartingWith("cloned:")).thenReturn(emptyList())
        whenever(preferences.findDistinctVoiceProfilesStartingWith("cloned:")).thenReturn(emptyList())
        whenever(narrations.findAllByVoiceProfileIn(any())).thenReturn(emptyList())
        whenever(storage.listNarrationKeys(any())).thenReturn(
            sequenceOf(
                "stories/71/ta/cloned_12.timings.json",
                "stories/71/ta/cloned_20.timings.json",
                "stories/71/ta/v1.timings.json",
            )
        )
        whenever(storage.deleteNarrationAudio(any())).thenReturn(true)

        service.reconcileOrphans()

        verify(storage).deleteNarrationAudio("stories/71/ta/cloned_12.timings.json")
        verify(storage, never()).deleteNarrationAudio("stories/71/ta/cloned_20.timings.json")
        verify(storage, never()).deleteNarrationAudio("stories/71/ta/v1.timings.json")
    }

    @Test
    fun `failed file delete is retried`() {
        whenever(storage.deleteNarrationAudio("stories/71/ta/cloned_12.mp3")).thenReturn(false, true)

        val failed = service.deleteFiles(listOf("stories/71/ta/cloned_12.mp3"))

        assertThat(failed).isZero
        verify(storage, times(2)).deleteNarrationAudio("stories/71/ta/cloned_12.mp3")
    }

    @Test
    fun `sweep removes rows, settings and files of deleted voices and keeps those of existing voices`() {
        whenever(voices.findById(12L)).thenReturn(null)
        whenever(voices.findById(20L)).thenReturn(mock<VoiceProfile>())
        whenever(narrations.findAllByVoiceProfileStartingWith("cloned:"))
            .thenReturn(listOf(row(1, "cloned:12", "stories/71/ta/cloned_12.mp3"), row(3, "cloned:20", "stories/71/ta/cloned_20.mp3")))
        whenever(voices.findById(9L)).thenReturn(null)
        whenever(preferences.findDistinctVoiceProfilesStartingWith("cloned:")).thenReturn(listOf("cloned:9", "cloned:20"))
        whenever(narrations.findAllByVoiceProfileIn(listOf("cloned:12", "cloned:9"))).thenReturn(listOf(row(1, "cloned:12", "stories/71/ta/cloned_12.mp3")))
        whenever(storage.listNarrationKeys(any())).thenReturn(
            sequenceOf("stories/71/ta/cloned_12.mp3", "stories/6/en/cloned_12.mp3", "stories/71/ta/cloned_20.mp3", "stories/71/ta/v1.mp3")
        )
        whenever(storage.deleteNarrationAudio(any())).thenReturn(true)

        service.reconcileOrphans()

        verify(narrations).deleteAllById(listOf(1L))
        verify(preferences).deleteByVoiceProfileIn(listOf("cloned:12", "cloned:9"))
        verify(storage).deleteNarrationAudio("stories/71/ta/cloned_12.mp3")
        verify(storage).deleteNarrationAudio("stories/6/en/cloned_12.mp3")
        verify(storage, never()).deleteNarrationAudio("stories/71/ta/cloned_20.mp3")
        verify(storage, never()).deleteNarrationAudio("stories/71/ta/v1.mp3")
    }

    private fun row(id: Long, voiceProfile: String, key: String) = StoryNarrationAudio(
        id = id, translationId = 1, voiceProfile = voiceProfile, audioUrl = key,
        durationSeconds = 100, status = NarrationAudioStatus.READY, createdAt = Instant.now()
    )
}
