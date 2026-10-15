package com.tamixa.application.stream

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.application.narration.AudioStorageService
import com.tamixa.application.narration.SSMLBuilderService
import com.tamixa.application.narration.TTSService
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.VoiceProfile
import com.tamixa.infrastructure.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/** Interactive segments in the parent's own voice: only their voice, only published stories, made once and reused. */
class InteractiveSegmentVoiceServiceTest {

    private val props = AppProperties(audio = AppProperties.AudioProperties(publicBaseUrl = "http://api.test", linkSigningSecret = "test-secret"))
    private val storyLibraryService: StoryLibraryService = mock()
    private val storyLibraryRepository: StoryLibraryRepositoryPort = mock()
    private val voices: VoiceRepositoryPort = mock()
    private val guard: SubscriptionGuard = mock()
    private val ssml: SSMLBuilderService = mock()
    private val signer = AudioLinkSigner(props)
    private val tts: TTSService = mock()
    private val storage: AudioStorageService = mock()
    private val service = InteractiveSegmentVoiceService(props, storyLibraryService, storyLibraryRepository, voices, guard, ssml, signer, tts, storage)

    private val graph = ObjectMapper().readTree("""{"startSegmentId":"hook","segments":{"hook":{"text":"மோகன் வீட்டுக்கு வந்தான்.","audioUrl":"x","choices":[]}}}""")

    private fun publishedStory(status: String = "PUBLISHED"): LibraryStoryResponse = mock<LibraryStoryResponse>().also {
        whenever(it.status).thenReturn(status)
        whenever(it.narrationApprovedAt).thenReturn(Instant.now())
        whenever(it.interactiveGraph).thenReturn(graph)
    }

    private fun givenOwnVoiceAndStory(status: String = "PUBLISHED") {
        whenever(voices.findByIdAndParentId(14L, 8L)).thenReturn(mock<VoiceProfile>())
        val story = publishedStory(status)
        whenever(storyLibraryService.findByIdAndLanguage(eq(73L), eq("ta"), any())).thenReturn(story)
        val master = mock<LibraryStory>()
        whenever(master.age).thenReturn(8)
        whenever(storyLibraryRepository.findById(73L)).thenReturn(master)
        whenever(ssml.buildSSML(any(), any(), any(), any())).thenReturn("<speak>text</speak>")
    }

    @Test
    fun `someone else's voice is refused without making audio`() {
        whenever(voices.findByIdAndParentId(14L, 8L)).thenReturn(null)

        val result = service.segmentUrl(73, "ta", "hook", "cloned:14", 8)

        assertThat(result).isEqualTo(InteractiveSegmentVoiceService.Result.NotFound)
        verify(tts, never()).synthesize(any(), any(), any())
    }

    @Test
    fun `unpublished story is refused`() {
        givenOwnVoiceAndStory(status = "AUDIO_REVIEW")

        assertThat(service.segmentUrl(73, "ta", "hook", "cloned:14", 8)).isEqualTo(InteractiveSegmentVoiceService.Result.NotFound)
        verify(tts, never()).synthesize(any(), any(), any())
    }

    @Test
    fun `first request makes the audio in the parent's voice and returns a signed private link`() {
        givenOwnVoiceAndStory()
        whenever(storage.listNarrationKeys(any())).thenReturn(emptySequence())
        whenever(tts.synthesize(any(), eq("ta"), eq("cloned:14"))).thenReturn(ByteArray(50_000))
        whenever(storage.uploadNarrationAudio(any(), any(), any(), any())).thenAnswer { "stories/73/ta/${it.arguments[2]}.mp3" }

        val result = service.segmentUrl(73, "ta", "hook", "cloned:14", 8)

        val slug = argumentCaptor<String>()
        verify(storage).uploadNarrationAudio(eq(73L), eq("ta"), slug.capture(), any())
        assertThat(slug.firstValue).matches("cloned_14_seg_hook_[0-9a-f]{8}")
        val url = (result as InteractiveSegmentVoiceService.Result.Ready).url
        val key = "stories/73/ta/${slug.firstValue}.mp3"
        assertThat(url).startsWith("http://api.test/audio/$key?exp=")
        // Private like other cloned narrations: the /audio proxy only serves cloned_ files with a valid signature.
        assertThat(signer.requiresSignature(key)).isTrue
        val exp = url.substringAfter("exp=").substringBefore("&")
        assertThat(signer.isValid(key, exp, url.substringAfter("sig="))).isTrue
    }

    @Test
    fun `audio made earlier is reused`() {
        givenOwnVoiceAndStory()
        whenever(storage.listNarrationKeys(any())).thenAnswer { sequenceOf(it.arguments[0] as String) }

        val result = service.segmentUrl(73, "ta", "hook", "cloned:14", 8)

        assertThat(result).isInstanceOf(InteractiveSegmentVoiceService.Result.Ready::class.java)
        verify(tts, never()).synthesize(any(), any(), any())
    }

    @Test
    fun `voice service failure is reported, not stored`() {
        givenOwnVoiceAndStory()
        whenever(storage.listNarrationKeys(any())).thenReturn(emptySequence())
        whenever(tts.synthesize(any(), any(), any())).thenReturn(null)

        assertThat(service.segmentUrl(73, "ta", "hook", "cloned:14", 8)).isEqualTo(InteractiveSegmentVoiceService.Result.Failed)
        verify(storage, never()).uploadNarrationAudio(any(), any(), any(), any())
    }

    @Test
    fun `unknown segment is refused`() {
        givenOwnVoiceAndStory()

        assertThat(service.segmentUrl(73, "ta", "nope", "cloned:14", 8)).isEqualTo(InteractiveSegmentVoiceService.Result.NotFound)
    }
}
