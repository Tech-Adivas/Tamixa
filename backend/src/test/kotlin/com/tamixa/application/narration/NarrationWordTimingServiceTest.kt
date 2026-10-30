package com.tamixa.application.narration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tamixa.application.port.ProxyStoragePort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.AlignedWord
import com.tamixa.application.port.narration.SpeechAlignmentPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationAudio
import com.tamixa.domain.narration.StoryNarrationScript
import com.tamixa.domain.narration.ToneMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Read-along word timings: served from the stored timings file while it matches the MP3, aligned again in the
 * background when missing or stale, and only for the audio file actually being played. Storage and alignment are fakes.
 */
class NarrationWordTimingServiceTest {

    private val mapper = jacksonObjectMapper()
    private val audioKey = "stories/75/ta/v1.mp3"
    private val timingsKey = "stories/75/ta/v1.timings.json"
    private val servedUrl = "/audio/stories/75/ta/v1.mp3?v=123"
    private val mp3 = ByteArray(1000) { 1 }
    private val alignedWords = listOf(AlignedWord("மழை", 0.14, 0.44), AlignedWord("நின்று", 0.5, 0.9))

    private val translation = StoryTranslation(
        id = 63, masterStoryId = 75, language = "ta", title = "t", content = "மழை நின்று", moral = null,
        wordCount = 2, readingTimeMinutes = 1.0, createdAt = Instant.now()
    )
    private val translations = mock<StoryTranslationRepositoryPort> {
        on { findByMasterStoryIdAndLanguage(75, "ta") } doReturn translation
        on { findById(63) } doReturn translation
    }
    private val audioRows = mock<StoryNarrationAudioRepositoryPort> {
        on { findByTranslationIdAndVoiceProfile(63, "default") } doReturn
            StoryNarrationAudio(97, 63, "default", audioKey, 553, NarrationAudioStatus.READY, Instant.now())
    }
    private val scripts = mock<StoryNarrationScriptRepositoryPort> {
        on { findByTranslationId(63) } doReturn
            StoryNarrationScript(1, 63, ToneMode.CALM, "மழை [Pause 500ms] நின்று", 100, Instant.now())
    }

    /** In-memory bucket; [stored] counts down when a timings file is written. */
    private inner class FakeStorage : ProxyStoragePort, AudioStorageService {
        val objects = ConcurrentHashMap<String, ByteArray>(mapOf(audioKey to mp3))
        val stored = CountDownLatch(1)
        override fun getObject(key: String) = objects[key]
        override fun getContentLength(key: String) = objects[key]?.size?.toLong()
        override fun getObjectRange(key: String, start: Long, end: Long): ByteArray? = null
        override fun uploadNarrationAudio(storyId: Long, language: String, voiceProfile: String, mp3Bytes: ByteArray) = ""
        override fun putNarrationObject(key: String, bytes: ByteArray, contentType: String): Boolean {
            objects[key] = bytes
            stored.countDown()
            return true
        }
        override fun deleteNarrationAudio(key: String) = true
        override fun listNarrationKeys(prefix: String) = emptySequence<String>()
    }

    private val storage = FakeStorage()
    private var alignedTranscript: String? = null
    private val alignment = object : SpeechAlignmentPort {
        override fun alignWords(audio: ByteArray, transcript: String): List<AlignedWord> {
            alignedTranscript = transcript
            return alignedWords
        }
    }

    private fun service(align: SpeechAlignmentPort = alignment) = NarrationWordTimingService(
        translations, audioRows, scripts, mapper, align, storage, storage
    )

    private fun storeTimings(audioBytes: Long) {
        storage.objects[timingsKey] = mapper.writeValueAsBytes(
            NarrationWordTimingService.StoredTimings(audioKey, audioBytes, alignedWords)
        )
    }

    @Test
    fun `serves stored timings while they match the audio file`() {
        storeTimings(mp3.size.toLong())
        val failingAlignment = mock<SpeechAlignmentPort>()

        val words = service(failingAlignment).getLibraryWordTimings(75, "ta", "default", servedUrl)

        assertThat(words).isEqualTo(alignedWords)
        verify(failingAlignment, never()).alignWords(any(), any())
    }

    @Test
    fun `missing timings return null and are aligned from the spoken script in the background`() {
        val svc = service()

        assertThat(svc.getLibraryWordTimings(75, "ta", "default", servedUrl)).isNull()
        assertThat(storage.stored.await(5, TimeUnit.SECONDS)).isTrue()

        val saved = mapper.readValue(storage.objects[timingsKey], NarrationWordTimingService.StoredTimings::class.java)
        assertThat(saved.audioBytes).isEqualTo(mp3.size.toLong())
        assertThat(saved.words).isEqualTo(alignedWords)
        assertThat(alignedTranscript).isEqualTo("மழை நின்று")
        assertThat(svc.getLibraryWordTimings(75, "ta", "default", servedUrl)).isEqualTo(alignedWords)
    }

    @Test
    fun `timings for an older version of the audio are realigned`() {
        storeTimings(audioBytes = 42)

        assertThat(service().getLibraryWordTimings(75, "ta", "default", servedUrl)).isNull()
        assertThat(storage.stored.await(5, TimeUnit.SECONDS)).isTrue()
        val saved = mapper.readValue(storage.objects[timingsKey], NarrationWordTimingService.StoredTimings::class.java)
        assertThat(saved.audioBytes).isEqualTo(mp3.size.toLong())
    }

    @Test
    fun `cloned voice that fell back to default narration gets the default timings`() {
        storeTimings(mp3.size.toLong())

        assertThat(service().getLibraryWordTimings(75, "ta", "cloned:19", servedUrl)).isEqualTo(alignedWords)
    }

    @Test
    fun `no timings when the served file is different audio`() {
        storeTimings(mp3.size.toLong())

        val familyUrl = "https://bucket.s3.amazonaws.com/family/8/75/ta.mp3?X-Amz-Signature=x"
        assertThat(service().getLibraryWordTimings(75, "ta", "family", familyUrl)).isNull()
    }

    @Test
    fun `timings file sits next to the mp3`() {
        assertThat(NarrationWordTimingService.timingsKeyFor("stories/72/ta/cloned_19.mp3"))
            .isEqualTo("stories/72/ta/cloned_19.timings.json")
    }
}
