package com.tamixa.application.narration.impl

import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.domain.VoiceProfile
import com.tamixa.infrastructure.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * First cloned narration of a voice without a cloud clone: the clone is created from the stored recording once,
 * even when several narrations start at the same time. Provider ports are fakes; no real API is called.
 */
class ClonedVoiceStrategyCreateFromReferenceTest {

    private val profileId = 7L
    private val referencePath = "voices/8/cloning_1_ref_my_voice_1.m4a"

    private val profiles = InMemoryVoiceRepository().apply {
        save(VoiceProfile(id = profileId, parentId = 8, encryptedEmbedding = ByteArray(0), createdAt = Instant.now(), referenceAudioPath = referencePath))
    }
    private val storage = mock<VoiceReferenceStoragePort> { on { getReferenceAudio(any()) } doReturn byteArrayOf(1, 2, 3) }
    private val props = AppProperties(
        voiceCloning = AppProperties.VoiceCloningProperties(enabled = true, provider = "elevenlabs", elevenLabsApiKey = "test-key")
    )

    private fun strategy(elevenLabs: ElevenLabsVoiceCloningPort) = ClonedVoiceStrategy(
        appProperties = props,
        voiceRepository = profiles,
        googleCloud = null,
        elevenLabs = elevenLabs,
        fishAudio = null,
        heyGenTts = null,
        selfHosted = null,
        voiceReferenceStorage = storage
    )

    @Test
    fun `concurrent first narrations create one ElevenLabs voice and all get audio`() {
        val elevenLabs = FakeElevenLabs(addVoiceDelayMs = 200)
        val strategy = strategy(elevenLabs)
        val callers = 4
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(callers)
        val results = (1..callers).map {
            pool.submit<ByteArray?> {
                start.await()
                strategy.synthesize("Once upon a time", "en", "cloned:$profileId")
            }
        }
        start.countDown()
        val audio = results.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertThat(elevenLabs.created.get()).isEqualTo(1)
        assertThat(audio).allSatisfy { assertThat(it).isNotNull().isNotEmpty() }
        assertThat(profiles.findById(profileId)!!.elevenlabsVoiceId).isEqualTo("voice-1")
        assertThat(elevenLabs.deleted).isEmpty()
    }

    @Test
    fun `a new voice that cannot synthesize is deleted and not saved`() {
        val elevenLabs = FakeElevenLabs(synthesizes = false)

        val audio = strategy(elevenLabs).synthesize("Once upon a time", "en", "cloned:$profileId")

        assertThat(audio).isNull()
        assertThat(elevenLabs.deleted).containsExactly("voice-1")
        assertThat(profiles.findById(profileId)!!.elevenlabsVoiceId).isNull()
    }

    private class FakeElevenLabs(
        private val addVoiceDelayMs: Long = 0,
        private val synthesizes: Boolean = true
    ) : ElevenLabsVoiceCloningPort {
        val created = AtomicInteger()
        val deleted: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())

        override fun addVoice(audioBytes: ByteArray, fileName: String, name: String): String {
            Thread.sleep(addVoiceDelayMs)
            return "voice-${created.incrementAndGet()}"
        }

        override fun synthesize(text: String, voiceId: String, language: String): ByteArray? =
            if (synthesizes) byteArrayOf(9, 9) else null

        override fun deleteVoice(voiceId: String): Boolean {
            deleted += voiceId
            return true
        }

        override fun hadRecentAuthFailure(voiceId: String) = false
        override fun hadRecentQuotaFailure(voiceId: String) = false
        override fun recentFailureMessage(voiceId: String): String? = null
    }

    private class InMemoryVoiceRepository : VoiceRepositoryPort {
        private val byId = ConcurrentHashMap<Long, VoiceProfile>()
        override fun save(voiceProfile: VoiceProfile): VoiceProfile = voiceProfile.also { byId[it.id] = it }
        override fun findById(id: Long): VoiceProfile? = byId[id]
        override fun findByParentId(parentId: Long): List<VoiceProfile> = byId.values.filter { it.parentId == parentId }
        override fun findByIdAndParentId(id: Long, parentId: Long): VoiceProfile? = byId[id]?.takeIf { it.parentId == parentId }
        override fun deleteById(id: Long) {
            byId.remove(id)
        }
    }
}
